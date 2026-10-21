package com.condoeconomy.api.core.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.condoeconomy.api.core.tenant.TenantContext;
import com.condoeconomy.api.core.tenant.TenantIdentifierResolver;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String ISSUER = "condoeconomy-api";
    private static final Duration VALIDADE = Duration.ofHours(2);

    @Value("${api.security.token.secret}")
    private String secret;

    @PostConstruct
    void validarSecret() {
        // HMAC-SHA256 exige no mínimo 256 bits (32 bytes) de chave para ser considerado seguro
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("api.security.token.secret deve ter pelo menos 32 caracteres. Defina a variável de ambiente JWT_SECRET.");
        }
        if (secret.contains("dev-only")) {
            log.warn("ATENÇÃO: usando a chave JWT padrão de desenvolvimento. Defina JWT_SECRET antes de ir para produção.");
        }
    }

    public String generateToken(UsuarioJpaEntity usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            String currentTenant = TenantContext.getCurrentTenant();
            if (currentTenant == null || currentTenant.isBlank()) {
                currentTenant = TenantIdentifierResolver.DEFAULT_TENANT;
            }
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(usuario.getEmail())
                    .withClaim("papel", usuario.getPapel())
                    .withClaim("tokenVersion", usuario.getTokenVersion())
                    .withClaim("tenantId", currentTenant)
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new IllegalStateException("Erro ao gerar token JWT", exception);
        }
    }

    public DecodedJWT verifyAndDecode(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build()
                    .verify(token);
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    public String validateToken(String token) {
        DecodedJWT decoded = verifyAndDecode(token);
        return decoded != null ? decoded.getSubject() : null;
    }

    private Instant genExpirationDate() {
        return Instant.now().plus(VALIDADE);
    }
}

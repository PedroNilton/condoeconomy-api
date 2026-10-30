package com.condoeconomy.api.core.security;

import com.condoeconomy.api.core.tenant.TenantContext;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.presentation.controller.AuthController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecurityFilter.class);

    private final TokenService tokenService;
    private final SpringDataUsuarioRepository usuarioRepository;
    private final SecurityAuditService auditService;

    public SecurityFilter(TokenService tokenService,
                          SpringDataUsuarioRepository usuarioRepository,
                          SecurityAuditService auditService) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        AtomicBoolean bloqueadoPorTenantSpoofing = new AtomicBoolean(false);

        if (token != null) {
            var decoded = tokenService.verifyAndDecode(token);
            if (decoded != null) {
                var email = decoded.getSubject();
                var claimVersion = decoded.getClaim("tokenVersion");
                Integer tokenVersion = claimVersion.isNull() ? null : claimVersion.asInt();
                var claimTenant = decoded.getClaim("tenantId");
                String tokenTenant = claimTenant.isNull() ? null : claimTenant.asString();

                if (email != null) {
                    usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                        // Barreira 3 (Confinamento Multi-Condomínio / Tenant Sandbox):
                        // Impede que um usuário autenticado de um condomínio envie header X-Tenant-ID de outro condomínio
                        String headerTenant = request.getHeader("X-Tenant-ID");
                        boolean isSuperAdmin = "ROLE_SUPER_ADMIN".equals(usuario.getPapel());
                        if (!isSuperAdmin && tokenTenant != null && headerTenant != null && !headerTenant.isBlank()
                                && !headerTenant.trim().equalsIgnoreCase(tokenTenant)) {
                            log.warn("[AUDITORIA DE SEGURANÇA] Bloqueio de Tenant Spoofing: usuário '{}' (tenant='{}') tentou acessar tenant='{}'",
                                    email, tokenTenant, headerTenant);
                            auditService.registrarUsuario(usuario, "TENTATIVA_SPOOFING_TENANT", "CRITICO",
                                    request.getRemoteAddr(),
                                    "Tentativa de acesso cruzado ao schema/condomínio: " + SecuritySanitizer.sanitizar(headerTenant));
                            bloqueadoPorTenantSpoofing.set(true);
                            return;
                        }

                        if (!isSuperAdmin && tokenTenant != null && !tokenTenant.isBlank()) {
                            TenantContext.setCurrentTenant(tokenTenant);
                        }

                        // Barreira 5 (Token Kill Switch & Bloqueio Imediato de Conta Desativada):
                        if (!usuario.isEnabled()) {
                            log.warn("[AUDITORIA DE SEGURANÇA] Tentativa de uso de token por conta desativada: '{}'", email);
                            return;
                        }
                        if (tokenVersion == null || tokenVersion == usuario.getTokenVersion()) {
                            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } else {
                            log.warn("[AUDITORIA DE SEGURANÇA] Token revogado interceptado para '{}' (tokenVersion={} != atual={})",
                                    email, tokenVersion, usuario.getTokenVersion());
                        }
                    });
                }
            }
        }

        if (bloqueadoPorTenantSpoofing.get()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Acesso negado: violação de confinamento de condomínio (Tenant Sandbox) detectada.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            var token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        // Fallback seguro: lê o token a partir do Cookie HttpOnly (HABITOS_SESSION)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AuthController.SESSION_COOKIE_NAME.equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue().trim();
                }
            }
        }
        return null;
    }
}

package com.condoeconomy.api.core.security;

import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final SpringDataUsuarioRepository usuarioRepository;

    public SecurityFilter(TokenService tokenService, SpringDataUsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        if (token != null) {
            var decoded = tokenService.verifyAndDecode(token);
            if (decoded != null) {
                var email = decoded.getSubject();
                var claimVersion = decoded.getClaim("tokenVersion");
                Integer tokenVersion = claimVersion.isNull() ? null : claimVersion.asInt();

                if (email != null) {
                    usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                        // Barreira de revogação imediata (Token Kill Switch):
                        // Se o tokenVersion for nulo (token legado pré-migração) ou igual à versão atual, autentica.
                        // Caso a senha tenha sido alterada ou sessões encerradas, a versão diverge e o acesso é cortado na hora.
                        if (tokenVersion == null || tokenVersion == usuario.getTokenVersion()) {
                            var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    });
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        var token = authHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}

package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.core.security.LoginAttemptService;
import com.condoeconomy.api.core.security.TokenService;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthenticationManager authenticationManager,
                          TokenService tokenService,
                          LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
    }

    public record LoginRequestDTO(
            @NotBlank(message = "O e-mail é obrigatório")
            @Size(max = 255, message = "E-mail não pode ultrapassar 255 caracteres")
            @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Formato de e-mail inválido ou caracteres não permitidos"
            )
            String email,

            @NotBlank(message = "A senha é obrigatória")
            @Size(min = 6, max = 128, message = "A senha deve ter entre 6 e 128 caracteres")
            String senha) {}

    public record LoginResponseDTO(String token, String papel, String nome) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO data, HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        // 1. Defesa ativa contra injeções (SQLi, XSS, scripts ou bytes nulos)
        if (SecuritySanitizer.contemPadroesMaliciosos(data.email()) || SecuritySanitizer.contemPadroesMaliciosos(data.senha())) {
            log.warn("Alerta de segurança: Tentativa de injeção interceptada na autenticação. IP: {}, Identificador fornecido: {}", ip, data.email());
            loginAttemptService.registrarFalha(data.email(), ip);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Entrada inválida ou caracteres proibidos detectados."));
        }

        // 2. Proteção contra ataques de força bruta
        if (loginAttemptService.estaBloqueado(data.email(), ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Muitas tentativas de login. Tente novamente em alguns minutos."));
        }

        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.senha());
            var auth = this.authenticationManager.authenticate(usernamePassword);

            var usuario = (UsuarioJpaEntity) auth.getPrincipal();
            var token = tokenService.generateToken(usuario);
            loginAttemptService.registrarSucesso(data.email(), ip);

            return ResponseEntity.ok(new LoginResponseDTO(token, usuario.getPapel(), usuario.getNome()));
        } catch (AuthenticationException e) {
            loginAttemptService.registrarFalha(data.email(), ip);
            // Mensagem genérica: não revela se o e-mail existe ou se a senha está errada
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "E-mail ou senha inválidos."));
        }
    }
}

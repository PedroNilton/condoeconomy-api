package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.core.security.LoginAttemptService;
import com.condoeconomy.api.core.security.TokenService;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 128) String senha) {}

    public record LoginResponseDTO(String token, String papel, String nome) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO data, HttpServletRequest request) {
        String ip = request.getRemoteAddr();

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

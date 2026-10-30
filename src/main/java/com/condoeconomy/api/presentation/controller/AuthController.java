package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.core.security.LoginAttemptService;
import com.condoeconomy.api.core.security.PasswordRecoveryService;
import com.condoeconomy.api.core.security.SecurityAuditService;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.core.security.TokenService;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public static final String SESSION_COOKIE_NAME = "HABITOS_SESSION";
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditService auditService;
    private final PasswordRecoveryService passwordRecoveryService;

    public AuthController(AuthenticationManager authenticationManager,
                          TokenService tokenService,
                          LoginAttemptService loginAttemptService,
                          SecurityAuditService auditService,
                          PasswordRecoveryService passwordRecoveryService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
        this.auditService = auditService;
        this.passwordRecoveryService = passwordRecoveryService;
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

    public record RecuperarSenhaRequestDTO(
            @NotBlank(message = "O e-mail é obrigatório")
            @Size(max = 255, message = "E-mail não pode ultrapassar 255 caracteres")
            @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Formato de e-mail inválido"
            )
            String email) {}

    public record RedefinirSenhaRequestDTO(
            @NotBlank(message = "O e-mail é obrigatório")
            @Size(max = 255, message = "E-mail não pode ultrapassar 255 caracteres")
            @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Formato de e-mail inválido"
            )
            String email,

            @NotBlank(message = "O código de verificação é obrigatório")
            @Pattern(regexp = "^\\d{6}$", message = "O código deve conter 6 dígitos numéricos")
            String codigo,

            @NotBlank(message = "A nova senha é obrigatória")
            @Size(min = 8, max = 128, message = "A nova senha deve ter entre 8 e 128 caracteres")
            String novaSenha) {}

    public record LoginResponseDTO(String token, String papel, String nome) {}

    public static ResponseCookie criarCookieSessao(String token, boolean secure, Duration maxAge) {
        return ResponseCookie.from(SESSION_COOKIE_NAME, token != null ? token : "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAge)
                .sameSite("Lax")
                .build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO data, HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        // 1. Defesa ativa contra injeções (SQLi, XSS, scripts ou bytes nulos)
        if (SecuritySanitizer.contemPadroesMaliciosos(data.email()) || SecuritySanitizer.contemPadroesMaliciosos(data.senha())) {
            log.warn("Alerta de segurança: Tentativa de injeção interceptada na autenticação. IP: {}, Identificador fornecido: {}", ip, data.email());
            loginAttemptService.registrarFalha(data.email(), ip);
            auditService.registrar(null, data.email(), "N/A", "TENTATIVA_INJECAO_LOGIN", "CRITICO", ip,
                    "Payload malicioso bloqueado na tela de login.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Entrada inválida ou caracteres proibidos detectados."));
        }

        // 2. Proteção contra ataques de força bruta
        if (loginAttemptService.estaBloqueado(data.email(), ip)) {
            auditService.registrar(null, data.email(), "N/A", "LOGIN_BLOQUEADO_FORCA_BRUTA", "ALERTA", ip,
                    "Acesso bloqueado temporariamente após múltiplas falhas consecutivas.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Muitas tentativas de login. Tente novamente em alguns minutos."));
        }

        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.senha());
            var auth = this.authenticationManager.authenticate(usernamePassword);

            var usuario = (UsuarioJpaEntity) auth.getPrincipal();
            var token = tokenService.generateToken(usuario);
            loginAttemptService.registrarSucesso(data.email(), ip);
            auditService.registrarUsuario(usuario, "LOGIN_SUCESSO", "INFO", ip,
                    "Autenticação realizada com sucesso (tokenVersion=" + usuario.getTokenVersion() + ").");

            ResponseCookie cookie = criarCookieSessao(token, request.isSecure(), Duration.ofHours(2));

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new LoginResponseDTO(token, usuario.getPapel(), usuario.getNome()));
        } catch (AuthenticationException e) {
            loginAttemptService.registrarFalha(data.email(), ip);
            auditService.registrar(null, data.email(), "N/A", "LOGIN_FALHA", "ALERTA", ip,
                    "Tentativa de login com credenciais inválidas.");
            // Mensagem genérica: não revela se o e-mail existe ou se a senha está errada
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "E-mail ou senha inválidos."));
        }
    }

    @PostMapping("/recuperar-senha")
    public ResponseEntity<Map<String, String>> recuperarSenha(@RequestBody @Valid RecuperarSenhaRequestDTO data,
                                                              HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!loginAttemptService.verificarEContabilizarRequisicaoPublica(ip, "recuperar-senha", 5)) {
            auditService.registrar(null, data.email(), "N/A", "FLOOD_RECUPERACAO_SENHA", "ALERTA", ip,
                    "Limite de solicitações de recuperação de senha excedido pelo IP.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Muitas solicitações recentes. Aguarde alguns minutos antes de tentar novamente."));
        }

        if (SecuritySanitizer.contemPadroesMaliciosos(data.email())) {
            auditService.registrar(null, data.email(), "N/A", "INJECAO_RECUPERACAO_SENHA", "CRITICO", ip,
                    "Payload suspeito interceptado em /recuperar-senha.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Endereço de e-mail inválido."));
        }

        passwordRecoveryService.solicitarRecuperacao(data.email(), ip);

        // Resposta uniforme para impedir enumeração de e-mails cadastrados
        return ResponseEntity.ok(Map.of(
                "message", "Se o e-mail estiver vinculado a uma conta ativa, um código de verificação de 6 dígitos foi enviado."
        ));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Map<String, String>> redefinirSenha(@RequestBody @Valid RedefinirSenhaRequestDTO data,
                                                              HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (!loginAttemptService.verificarEContabilizarRequisicaoPublica(ip, "redefinir-senha", 8)) {
            auditService.registrar(null, data.email(), "N/A", "FLOOD_REDEFINICAO_SENHA", "ALERTA", ip,
                    "Limite de tentativas de redefinição de senha excedido pelo IP.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Muitas tentativas de verificação. Aguarde alguns minutos."));
        }

        try {
            passwordRecoveryService.redefinirSenha(data.email(), data.codigo(), data.novaSenha(), ip);
            ResponseCookie clearCookie = criarCookieSessao("", request.isSecure(), Duration.ZERO);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                    .body(Map.of("message", "Senha redefinida com sucesso! Você já pode entrar com sua nova senha."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        ResponseCookie clearCookie = criarCookieSessao("", request.isSecure(), Duration.ZERO);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(Map.of("message", "Sessão encerrada e cookie HttpOnly removido com sucesso."));
    }
}

package com.condoeconomy.api.core.security;

import com.condoeconomy.api.application.service.notification.EmailService;
import com.condoeconomy.api.infrastructure.persistence.entity.TokenRecuperacaoSenhaJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataTokenRecuperacaoSenhaRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço seguro de Recuperação e Redefinição de Senha.
 * <p>
 * Princípios aplicados:
 * <ul>
 *   <li>Código OTP de 6 dígitos gerado via {@link SecureRandom}; apenas seu hash SHA-256 é persistido.</li>
 *   <li>Expiração curta (15 minutos) e uso único garantido transacionalmente.</li>
 *   <li>Resposta uniforme (anti-enumeração de contas) na solicitação de recuperação.</li>
 *   <li>Token Kill Switch: incrementa {@code tokenVersion} ao redefinir a senha, revogando sessões anteriores.</li>
 * </ul>
 */
@Service
public class PasswordRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private static final int EXPIRACAO_MINUTOS = 15;

    private final SpringDataUsuarioRepository usuarioRepository;
    private final SpringDataTokenRecuperacaoSenhaRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService auditService;
    private final String mailUsername;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordRecoveryService(SpringDataUsuarioRepository usuarioRepository,
                                   SpringDataTokenRecuperacaoSenhaRepository tokenRepository,
                                   EmailService emailService,
                                   PasswordEncoder passwordEncoder,
                                   SecurityAuditService auditService,
                                   @Value("${spring.mail.username:}") String mailUsername) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.mailUsername = mailUsername;
    }

    @Transactional
    public void solicitarRecuperacao(String emailBruto, String ip) {
        SecuritySanitizer.validarTextoSeguro(emailBruto, "email");
        String emailNormalizado = SecuritySanitizer.sanitizar(emailBruto).toLowerCase(Locale.ROOT);

        Optional<UsuarioJpaEntity> optUsuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado);
        if (optUsuario.isEmpty() || !optUsuario.get().isEnabled()) {
            auditService.registrar(null, emailNormalizado, "N/A",
                    "SOLICITACAO_RECUPERACAO_CONTA_INEXISTENTE_OU_INATIVA", "INFO", ip,
                    "Solicitação de recuperação recebida para e-mail não localizado ou desativado (resposta uniforme retornada).");
            return;
        }

        UsuarioJpaEntity usuario = optUsuario.get();
        tokenRepository.invalidarTokensAnteriores(usuario.getId());

        String codigo = String.format("%06d", secureRandom.nextInt(1_000_000));
        String tokenHash = gerarHashSha256(usuario.getEmail(), codigo);

        TokenRecuperacaoSenhaJpaEntity entidade = new TokenRecuperacaoSenhaJpaEntity(
                UUID.randomUUID(),
                usuario.getId(),
                tokenHash,
                LocalDateTime.now().plusMinutes(EXPIRACAO_MINUTOS),
                false,
                ip,
                LocalDateTime.now()
        );
        tokenRepository.save(entidade);

        String corpoHtml = """
                <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 12px;">
                    <h2 style="color: #111827; margin-top: 0;">Recuperação de Acesso — HabitOS</h2>
                    <p style="color: #4b5563; font-size: 14px;">Olá, <strong>%s</strong>.</p>
                    <p style="color: #4b5563; font-size: 14px;">Recebemos uma solicitação para redefinir sua senha. Utilize o código de verificação de uso único abaixo:</p>
                    <div style="background: #f3f4f6; border-radius: 8px; padding: 16px; text-align: center; margin: 20px 0;">
                        <span style="font-size: 28px; font-weight: bold; letter-spacing: 6px; color: #111827;">%s</span>
                    </div>
                    <p style="color: #6b7280; font-size: 12px;">Este código expira em <strong>%d minutos</strong>. Se você não solicitou esta alteração, ignore esta mensagem.</p>
                </div>
                """.formatted(SecuritySanitizer.sanitizar(usuario.getNome()), codigo, EXPIRACAO_MINUTOS);

        emailService.enviarEmail(usuario.getEmail(), "Código de Recuperação de Senha — HabitOS", corpoHtml);

        boolean smtpDev = mailUsername == null || mailUsername.isBlank() || mailUsername.equalsIgnoreCase("seuemail@gmail.com");
        if (smtpDev) {
            log.info("[MODO DEV/LOCAL] Código temporário de recuperação para '{}': {} (válido por {} min)",
                    usuario.getEmail(), codigo, EXPIRACAO_MINUTOS);
        }

        auditService.registrarUsuario(usuario, "SOLICITACAO_RECUPERACAO_SENHA", "INFO", ip,
                "Código temporário SHA-256 emitido com validade de " + EXPIRACAO_MINUTOS + " minutos.");
    }

    @Transactional
    public void redefinirSenha(String emailBruto, String codigoBruto, String novaSenha, String ip) {
        SecuritySanitizer.validarTextoSeguro(emailBruto, "email");
        SecuritySanitizer.validarTextoSeguro(codigoBruto, "codigo");

        if (SecuritySanitizer.contemPadroesMaliciosos(novaSenha)) {
            throw new IllegalArgumentException("A nova senha contém caracteres ou padrões proibidos.");
        }
        if (novaSenha == null || novaSenha.length() < 8 || novaSenha.length() > 128) {
            throw new IllegalArgumentException("A nova senha deve possuir entre 8 e 128 caracteres.");
        }

        String codigoLimpo = codigoBruto == null ? "" : codigoBruto.trim();
        if (!codigoLimpo.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("O código de verificação deve conter exatamente 6 dígitos numéricos.");
        }

        String emailNormalizado = SecuritySanitizer.sanitizar(emailBruto).toLowerCase(Locale.ROOT);
        Optional<UsuarioJpaEntity> optUsuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizado);

        if (optUsuario.isEmpty() || !optUsuario.get().isEnabled()) {
            auditService.registrar(null, emailNormalizado, "N/A", "FALHA_REDEFINICAO_SENHA", "ALERTA", ip,
                    "Tentativa de redefinição para conta inexistente ou desativada.");
            throw new IllegalArgumentException("Código de verificação inválido ou expirado.");
        }

        UsuarioJpaEntity usuario = optUsuario.get();
        String tokenHash = gerarHashSha256(usuario.getEmail(), codigoLimpo);

        Optional<TokenRecuperacaoSenhaJpaEntity> optToken = tokenRepository
                .findFirstByUsuarioIdAndTokenHashAndUsadoFalseOrderByCriadoEmDesc(usuario.getId(), tokenHash);

        if (optToken.isEmpty()) {
            auditService.registrarUsuario(usuario, "FALHA_REDEFINICAO_SENHA", "ALERTA", ip,
                    "Tentativa de redefinição com código incorreto ou já utilizado.");
            throw new IllegalArgumentException("Código de verificação inválido ou expirado.");
        }

        TokenRecuperacaoSenhaJpaEntity tokenEntity = optToken.get();
        if (tokenEntity.getExpiraEm().isBefore(LocalDateTime.now())) {
            tokenEntity.setUsado(true);
            tokenRepository.save(tokenEntity);
            auditService.registrarUsuario(usuario, "FALHA_REDEFINICAO_SENHA_EXPIRADO", "ALERTA", ip,
                    "Tentativa de uso de código temporário expirado.");
            throw new IllegalArgumentException("Código de verificação inválido ou expirado.");
        }

        tokenEntity.setUsado(true);
        tokenRepository.save(tokenEntity);

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setTokenVersion(usuario.getTokenVersion() + 1);
        usuarioRepository.save(usuario);

        auditService.registrarUsuario(usuario, "SENHA_REDEFINIDA_RECUPERACAO", "ALERTA", ip,
                "Senha redefinida com sucesso via OTP e sessões anteriores revogadas (tokenVersion=" + usuario.getTokenVersion() + ").");
    }

    static String gerarHashSha256(String email, String codigo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = email.trim().toLowerCase(Locale.ROOT) + ":" + codigo.trim();
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM", e);
        }
    }
}

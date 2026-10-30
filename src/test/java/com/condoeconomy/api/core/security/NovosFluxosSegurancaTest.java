package com.condoeconomy.api.core.security;

import com.condoeconomy.api.application.service.notification.EmailService;
import com.condoeconomy.api.infrastructure.persistence.entity.TokenRecuperacaoSenhaJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataTokenRecuperacaoSenhaRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.presentation.controller.admin.AdminUsuarioController;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NovosFluxosSegurancaTest {

    private SpringDataUsuarioRepository usuarioRepository;
    private SpringDataTokenRecuperacaoSenhaRepository tokenRepository;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private SecurityAuditService auditService;
    private PasswordRecoveryService passwordRecoveryService;
    private AdminUsuarioController adminUsuarioController;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(SpringDataUsuarioRepository.class);
        tokenRepository = mock(SpringDataTokenRecuperacaoSenhaRepository.class);
        emailService = mock(EmailService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        auditService = mock(SecurityAuditService.class);

        passwordRecoveryService = new PasswordRecoveryService(
                usuarioRepository,
                tokenRepository,
                emailService,
                passwordEncoder,
                auditService,
                "seuemail@gmail.com"
        );

        adminUsuarioController = new AdminUsuarioController(
                usuarioRepository,
                passwordEncoder,
                auditService
        );
    }

    @Test
    @DisplayName("Recuperação de Senha: gera hash SHA-256 sem expor código em texto puro no banco e incrementa tokenVersion na redefinição")
    void deveGerarHashSha256ERedefinirSenhaComKillSwitch() {
        UsuarioJpaEntity morador = new UsuarioJpaEntity();
        morador.setId(UUID.randomUUID());
        morador.setNome("Carlos Silva");
        morador.setEmail("carlos.silva@email.com");
        morador.setSenha(passwordEncoder.encode("senhaAntiga123"));
        morador.setPapel("ROLE_MORADOR");
        morador.setTokenVersion(2);
        morador.setAtivo(true);

        when(usuarioRepository.findByEmailIgnoreCase("carlos.silva@email.com")).thenReturn(Optional.of(morador));

        passwordRecoveryService.solicitarRecuperacao("carlos.silva@email.com", "127.0.0.1");

        ArgumentCaptor<TokenRecuperacaoSenhaJpaEntity> captor = ArgumentCaptor.forClass(TokenRecuperacaoSenhaJpaEntity.class);
        verify(tokenRepository).save(captor.capture());

        TokenRecuperacaoSenhaJpaEntity salvo = captor.getValue();
        assertEquals(64, salvo.getTokenHash().length(), "Deve persistir apenas hash SHA-256 hexadecimal de 64 chars");

        // Simula redefinição com código conhecido
        String codigoTeste = "482910";
        String hashEsperado = PasswordRecoveryService.gerarHashSha256("carlos.silva@email.com", codigoTeste);
        TokenRecuperacaoSenhaJpaEntity tokenValido = new TokenRecuperacaoSenhaJpaEntity(
                UUID.randomUUID(),
                morador.getId(),
                hashEsperado,
                LocalDateTime.now().plusMinutes(15),
                false,
                "127.0.0.1",
                LocalDateTime.now()
        );

        when(tokenRepository.findFirstByUsuarioIdAndTokenHashAndUsadoFalseOrderByCriadoEmDesc(
                eq(morador.getId()), eq(hashEsperado)))
                .thenReturn(Optional.of(tokenValido));

        passwordRecoveryService.redefinirSenha("carlos.silva@email.com", codigoTeste, "NovaSenhaForte2026", "127.0.0.1");

        assertTrue(tokenValido.isUsado(), "O token temporário deve ser marcado como usado imediatamente");
        assertEquals(3, morador.getTokenVersion(), "O tokenVersion deve incrementar para revogar sessões ativas");
        assertTrue(passwordEncoder.matches("NovaSenhaForte2026", morador.getSenha()));
        assertNotEquals("NovaSenhaForte2026", morador.getSenha());
    }

    @Test
    @DisplayName("Recuperação de Senha: não revela inexistência de e-mail nem dispara e-mail para conta inexistente")
    void deveManterRespostaSilenciosaParaEmailInexistente() {
        when(usuarioRepository.findByEmailIgnoreCase("inexistente@email.com")).thenReturn(Optional.empty());

        passwordRecoveryService.solicitarRecuperacao("inexistente@email.com", "127.0.0.1");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarEmail(any(), any(), any());
    }

    @Test
    @DisplayName("Recuperação de Senha: bloqueia código expirado")
    void deveRejeitarCodigoExpirado() {
        UsuarioJpaEntity morador = new UsuarioJpaEntity();
        morador.setId(UUID.randomUUID());
        morador.setEmail("carlos@email.com");
        morador.setAtivo(true);

        String codigo = "111222";
        String hash = PasswordRecoveryService.gerarHashSha256("carlos@email.com", codigo);
        TokenRecuperacaoSenhaJpaEntity tokenExpirado = new TokenRecuperacaoSenhaJpaEntity(
                UUID.randomUUID(),
                morador.getId(),
                hash,
                LocalDateTime.now().minusMinutes(1),
                false,
                "127.0.0.1",
                LocalDateTime.now().minusMinutes(16)
        );

        when(usuarioRepository.findByEmailIgnoreCase("carlos@email.com")).thenReturn(Optional.of(morador));
        when(tokenRepository.findFirstByUsuarioIdAndTokenHashAndUsadoFalseOrderByCriadoEmDesc(morador.getId(), hash))
                .thenReturn(Optional.of(tokenExpirado));

        assertThrows(IllegalArgumentException.class, () ->
                passwordRecoveryService.redefinirSenha("carlos@email.com", codigo, "NovaSenhaSegura99", "127.0.0.1")
        );
        assertTrue(tokenExpirado.isUsado());
    }

    @Test
    @DisplayName("Gestão de Usuários: impede escalação de privilégio quando Síndico tenta criar SUPER_ADMIN")
    void deveBloquearEscalacaoDePrivilegioPeloSindico() {
        UsuarioJpaEntity sindico = new UsuarioJpaEntity();
        sindico.setId(UUID.randomUUID());
        sindico.setEmail("sindico@condominio.com");
        sindico.setPapel("ROLE_SINDICO");

        var dto = new AdminUsuarioController.CriarUsuarioAdminRequestDTO(
                "Atacante",
                "atacante@email.com",
                "SenhaForte123",
                "ROLE_SUPER_ADMIN",
                null,
                "101",
                "A"
        );

        HttpServletRequest req = new MockHttpServletRequest();
        var resposta = adminUsuarioController.cadastrarUsuario(dto, sindico, req);

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Gestão de Usuários: bloquear morador desativa conta e incrementa tokenVersion imediatamente")
    void deveRevogarSessoesAoBloquearMorador() {
        UsuarioJpaEntity sindico = new UsuarioJpaEntity();
        sindico.setId(UUID.randomUUID());
        sindico.setEmail("sindico@condominio.com");
        sindico.setPapel("ROLE_SINDICO");

        UUID moradorId = UUID.randomUUID();
        UsuarioJpaEntity morador = new UsuarioJpaEntity();
        morador.setId(moradorId);
        morador.setNome("Morador Teste");
        morador.setEmail("morador@email.com");
        morador.setPapel("ROLE_MORADOR");
        morador.setTokenVersion(1);
        morador.setAtivo(true);

        when(usuarioRepository.findById(moradorId)).thenReturn(Optional.of(morador));
        when(usuarioRepository.save(any(UsuarioJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = adminUsuarioController.alterarStatusConta(
                moradorId,
                new AdminUsuarioController.AtualizarStatusUsuarioDTO(false),
                sindico,
                new MockHttpServletRequest()
        );

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertFalse(morador.isAtivo());
        assertEquals(2, morador.getTokenVersion(), "Bloquear usuário deve invalidar JWTs ativos via tokenVersion + 1");
    }
}

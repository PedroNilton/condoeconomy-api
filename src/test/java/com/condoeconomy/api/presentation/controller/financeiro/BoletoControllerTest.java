package com.condoeconomy.api.presentation.controller.financeiro;

import com.condoeconomy.api.application.usecase.financeiro.DarBaixaBoletoUseCase;
import com.condoeconomy.api.application.usecase.financeiro.ListarBoletosUseCase;
import com.condoeconomy.api.core.security.SecurityAuditService;
import com.condoeconomy.api.domain.entity.financeiro.Boleto;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.presentation.dto.financeiro.BoletoResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoletoControllerTest {

    @Mock
    private ListarBoletosUseCase listarBoletosUseCase;

    @Mock
    private DarBaixaBoletoUseCase darBaixaBoletoUseCase;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityAuditService auditService;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private BoletoController controller;

    private UsuarioJpaEntity sindico;
    private UsuarioJpaEntity morador;

    @BeforeEach
    void setUp() {
        sindico = new UsuarioJpaEntity();
        sindico.setId(UUID.randomUUID());
        sindico.setEmail("sindico@condominio.com");
        sindico.setPapel("ROLE_SINDICO");
        sindico.setSenha("$2a$10$hashedPasswordSindico");

        morador = new UsuarioJpaEntity();
        morador.setId(UUID.randomUUID());
        morador.setEmail("morador@condominio.com");
        morador.setPapel("ROLE_MORADOR");
        morador.setApartamento("302");
        morador.setBloco("B");

        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
    }

    @Test
    @DisplayName("Financeiro: Morador deve ter consulta confinada à própria unidade (Anti-IDOR)")
    void deveConfinarUnidadeParaMoradorEvitandoIdor() {
        when(authentication.getPrincipal()).thenReturn(morador);

        Boleto boleto = new Boleto(UUID.randomUUID(), "Apto 302 - Bloco B", "Morador Teste",
                "10/2026", new BigDecimal("450.00"), LocalDate.now().plusDays(5), null,
                Boleto.StatusBoleto.PENDENTE, "34191...", "http://pdf");

        // Morador tenta consultar "Apto 101 - Bloco A"
        when(listarBoletosUseCase.executar("Apto 302 - Bloco B")).thenReturn(List.of(boleto));

        ResponseEntity<List<BoletoResponseDTO>> response = controller.listarBoletos(authentication, "Apto 101 - Bloco A", request);

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Apto 302 - Bloco B", response.getBody().get(0).unidadeTexto());

        // Deve registrar auditoria de alerta para a tentativa de IDOR
        verify(auditService, times(1)).registrarUsuario(
                eq(morador),
                eq("TENTATIVA_IDOR_BOLETO"),
                eq("ALERTA"),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Financeiro: Síndico realiza baixa manual de boleto com Step-Up Auth válido")
    void deveRealizarBaixaDeBoletoComStepUpAuthValido() {
        when(authentication.getPrincipal()).thenReturn(sindico);
        when(passwordEncoder.matches("SenhaSecreta123", sindico.getSenha())).thenReturn(true);

        UUID boletoId = UUID.randomUUID();
        Boleto boletoPago = new Boleto(boletoId, "Apto 101 - Bloco A", "Carlos",
                "10/2026", new BigDecimal("500.00"), LocalDate.now().minusDays(1), LocalDate.now(),
                Boleto.StatusBoleto.PAGO, "34191...", "http://pdf");

        when(darBaixaBoletoUseCase.executar(boletoId)).thenReturn(boletoPago);

        ResponseEntity<?> response = controller.darBaixa(
                boletoId,
                "SenhaSecreta123",
                null,
                authentication,
                request
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(darBaixaBoletoUseCase, times(1)).executar(boletoId);
    }

    @Test
    @DisplayName("Financeiro: Deve bloquear baixa manual quando Step-Up Auth falhar e registrar auditoria")
    void deveRejeitarBaixaDeBoletoQuandoSenhaStepUpEstiverIncorreta() {
        when(authentication.getPrincipal()).thenReturn(sindico);
        when(passwordEncoder.matches("SenhaErrada", sindico.getSenha())).thenReturn(false);

        UUID boletoId = UUID.randomUUID();

        ResponseEntity<?> response = controller.darBaixa(
                boletoId,
                "SenhaErrada",
                null,
                authentication,
                request
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(darBaixaBoletoUseCase, never()).executar(any());
        verify(auditService, times(1)).registrarUsuario(
                eq(sindico),
                eq("FALHA_STEP_UP_BAIXA_BOLETO"),
                eq("ALERTA"),
                anyString(),
                anyString()
        );
    }
}

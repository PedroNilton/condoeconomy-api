package com.condoeconomy.api.presentation.controller.reserva;

import com.condoeconomy.api.application.usecase.reserva.*;
import com.condoeconomy.api.domain.entity.reserva.AreaComum;
import com.condoeconomy.api.domain.entity.reserva.Reserva;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.presentation.dto.reserva.CriarReservaRequestDTO;
import com.condoeconomy.api.presentation.dto.reserva.RejeitarReservaRequestDTO;
import com.condoeconomy.api.presentation.dto.reserva.ReservaResponseDTO;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    @Mock
    private CriarReservaUseCase criarReservaUseCase;

    @Mock
    private ListarReservasUseCase listarReservasUseCase;

    @Mock
    private ListarAreasComunsUseCase listarAreasComunsUseCase;

    @Mock
    private CheckInConvidadoUseCase checkInConvidadoUseCase;

    @Mock
    private AprovarRejeitarReservaUseCase aprovarRejeitarReservaUseCase;

    @Mock
    private ListarMinhasReservasUseCase listarMinhasReservasUseCase;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReservaController controller;

    private UsuarioJpaEntity morador;
    private AreaComum churrasqueira;

    @BeforeEach
    void setUp() {
        morador = new UsuarioJpaEntity();
        morador.setId(UUID.randomUUID());
        morador.setNome("Mariana Souza");
        morador.setEmail("mariana@condominio.com");
        morador.setApartamento("402");
        morador.setBloco("C");

        churrasqueira = new AreaComum(UUID.randomUUID(), "Churrasqueira 01", "Espaço Gourmet", 30, new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Reservas: Deve criar solicitação de reserva com sucesso e sanitizar dados")
    void deveCriarReservaComSucessoESanitizarDados() {
        when(authentication.getPrincipal()).thenReturn(morador);

        LocalDate data = LocalDate.now().plusDays(7);
        LocalTime inicio = LocalTime.of(12, 0);
        LocalTime fim = LocalTime.of(18, 0);

        CriarReservaRequestDTO request = new CriarReservaRequestDTO(
                churrasqueira.getId(),
                "Apto 402 - Bloco C",
                "Mariana Souza",
                "Almoço de Família",
                data,
                inicio,
                fim,
                List.of(new CriarReservaRequestDTO.ConvidadoInput("Pedro Souza", "123.456.789-00"))
        );

        Reserva reservaCriada = Reserva.criar(
                churrasqueira,
                "Apto 402 - Bloco C",
                "Mariana Souza",
                "Almoço de Família",
                data,
                inicio,
                fim,
                List.of()
        );

        when(criarReservaUseCase.executar(
                eq(churrasqueira.getId()),
                eq("Apto 402 - Bloco C"),
                eq("Mariana Souza"),
                eq("Almoço de Família"),
                eq(data),
                eq(inicio),
                eq(fim),
                any()
        )).thenReturn(reservaCriada);

        ResponseEntity<ReservaResponseDTO> response = controller.criarReserva(request, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Almoço de Família", response.getBody().titulo());
        assertEquals("Apto 402 - Bloco C", response.getBody().unidade());
        verify(criarReservaUseCase, times(1)).executar(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Reservas: Deve rejeitar criação de reserva quando título contiver injeção XSS")
    void deveRejeitarCriacaoDeReservaComScriptXssNoTitulo() {
        CriarReservaRequestDTO request = new CriarReservaRequestDTO(
                churrasqueira.getId(),
                "Apto 402 - Bloco C",
                "Mariana Souza",
                "<script>alert('xss')</script>",
                LocalDate.now().plusDays(2),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                List.of()
        );

        assertThrows(IllegalArgumentException.class, () ->
                controller.criarReserva(request, authentication));

        verify(criarReservaUseCase, never()).executar(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Reservas: Síndico deve aprovar reserva com sucesso")
    void deveAprovarReservaComSucesso() {
        UUID reservaId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.aprovar(reservaId, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(aprovarRejeitarReservaUseCase, times(1)).aprovar(reservaId);
    }

    @Test
    @DisplayName("Reservas: Síndico deve rejeitar reserva fornecendo motivo sanitizado")
    void deveRejeitarReservaComMotivoSanitizado() {
        UUID reservaId = UUID.randomUUID();
        RejeitarReservaRequestDTO dto = new RejeitarReservaRequestDTO("Área em manutenção técnica programada");

        ResponseEntity<Void> response = controller.rejeitar(reservaId, dto, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(aprovarRejeitarReservaUseCase, times(1)).rejeitar(reservaId, "Área em manutenção técnica programada");
    }
}

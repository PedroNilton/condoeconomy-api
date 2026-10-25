package com.condoeconomy.api.presentation.controller.portaria;

import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.VeiculoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataVeiculoRepository;
import com.condoeconomy.api.presentation.controller.portaria.dto.VeiculoConsultaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortariaVeiculoControllerTest {

    @Mock
    private SpringDataVeiculoRepository veiculoRepository;

    @Mock
    private SpringDataUsuarioRepository usuarioRepository;

    @InjectMocks
    private PortariaVeiculoController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "veiculoRepository", veiculoRepository);
        ReflectionTestUtils.setField(controller, "usuarioRepository", usuarioRepository);
    }

    @Test
    @DisplayName("Portaria: Deve consultar placa com sucesso e enriquecer com dados do morador")
    void deveConsultarVeiculoComSucessoEFormatarPlaca() {
        UUID moradorId = UUID.randomUUID();
        UsuarioJpaEntity morador = new UsuarioJpaEntity();
        morador.setId(moradorId);
        morador.setNome("Carlos Silva");
        morador.setApartamento("101");
        morador.setBloco("A");

        VeiculoJpaEntity veiculo = new VeiculoJpaEntity();
        veiculo.setId(UUID.randomUUID());
        veiculo.setPlaca("ABC-1234");
        veiculo.setModelo("Corolla");
        veiculo.setCor("Prata");
        veiculo.setUsuarioId(moradorId);

        when(veiculoRepository.findByPlacaContainingIgnoreCase("ABC-1234")).thenReturn(List.of(veiculo));
        when(usuarioRepository.findById(moradorId)).thenReturn(Optional.of(morador));

        // Busca sem traço "ABC1234" -> o controller deve formatar para "ABC-1234"
        ResponseEntity<List<VeiculoConsultaDTO>> response = controller.consultarVeiculo("ABC1234");

        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        VeiculoConsultaDTO dto = response.getBody().get(0);
        assertEquals("ABC-1234", dto.getPlaca());
        assertEquals("Carlos Silva", dto.getNomeMorador());
        assertEquals("101", dto.getApartamento());
        assertEquals("A", dto.getBloco());
    }

    @Test
    @DisplayName("Portaria: Deve bloquear consulta com tentativa de injeção SQL na placa")
    void deveRejeitarConsultaComPlacaInseguraOuSqlInjection() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.consultarVeiculo("' OR 1=1 --"));
    }

    @Test
    @DisplayName("Portaria: Deve retornar lista vazia caso veículo não esteja cadastrado")
    void deveRetornarListaVaziaCasoNaoEncontrado() {
        when(veiculoRepository.findByPlacaContainingIgnoreCase(anyString())).thenReturn(List.of());

        ResponseEntity<List<VeiculoConsultaDTO>> response = controller.consultarVeiculo("XYZ-9999");

        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }
}

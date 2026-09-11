package com.condoeconomy.api.presentation.controller.financeiro;

import com.condoeconomy.api.application.usecase.financeiro.DarBaixaBoletoUseCase;
import com.condoeconomy.api.application.usecase.financeiro.ListarBoletosUseCase;
import com.condoeconomy.api.presentation.dto.financeiro.BoletoResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/boletos")
public class BoletoController {

    private final ListarBoletosUseCase listarBoletosUseCase;
    private final DarBaixaBoletoUseCase darBaixaBoletoUseCase;

    public BoletoController(ListarBoletosUseCase listarBoletosUseCase, DarBaixaBoletoUseCase darBaixaBoletoUseCase) {
        this.listarBoletosUseCase = listarBoletosUseCase;
        this.darBaixaBoletoUseCase = darBaixaBoletoUseCase;
    }

    @GetMapping
    public ResponseEntity<List<BoletoResponseDTO>> listarBoletos(
            Authentication auth,
            @RequestParam(required = false) String unidade) {
        
        com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity user = 
            (com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity) auth.getPrincipal();
            
        String unidadeConsulta = unidade;
        
        // Se for morador, obriga a buscar SOMENTE da própria unidade
        if ("ROLE_MORADOR".equals(user.getPapel())) {
            unidadeConsulta = "Apto " + user.getApartamento() + " - Bloco " + user.getBloco();
        }

        var boletos = listarBoletosUseCase.executar(unidadeConsulta);
        var dtos = boletos.stream().map(BoletoResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/baixar")
    public ResponseEntity<BoletoResponseDTO> darBaixa(@PathVariable UUID id) {
        var boleto = darBaixaBoletoUseCase.executar(id);
        return ResponseEntity.ok(BoletoResponseDTO.fromEntity(boleto));
    }
}

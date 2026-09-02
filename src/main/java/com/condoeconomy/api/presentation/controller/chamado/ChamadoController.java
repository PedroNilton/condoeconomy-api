package com.condoeconomy.api.presentation.controller.chamado;

import com.condoeconomy.api.application.usecase.chamado.AtualizarStatusChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.CriarChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.ListarChamadosUseCase;
import com.condoeconomy.api.domain.entity.chamado.Chamado;
import com.condoeconomy.api.presentation.dto.chamado.AtualizarStatusChamadoRequestDTO;
import com.condoeconomy.api.presentation.dto.chamado.ChamadoResponseDTO;
import com.condoeconomy.api.presentation.dto.chamado.CriarChamadoRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chamados")
public class ChamadoController {

    private final ListarChamadosUseCase listarChamadosUseCase;
    private final AtualizarStatusChamadoUseCase atualizarStatusChamadoUseCase;
    private final CriarChamadoUseCase criarChamadoUseCase;

    public ChamadoController(ListarChamadosUseCase listarChamadosUseCase, 
                             AtualizarStatusChamadoUseCase atualizarStatusChamadoUseCase,
                             CriarChamadoUseCase criarChamadoUseCase) {
        this.listarChamadosUseCase = listarChamadosUseCase;
        this.atualizarStatusChamadoUseCase = atualizarStatusChamadoUseCase;
        this.criarChamadoUseCase = criarChamadoUseCase;
    }

    @GetMapping
    public ResponseEntity<List<ChamadoResponseDTO>> listarChamados() {
        var chamados = listarChamadosUseCase.executar();
        var dtos = chamados.stream().map(ChamadoResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<ChamadoResponseDTO> criarChamado(@Valid @RequestBody CriarChamadoRequestDTO request) {
        var chamado = criarChamadoUseCase.executar(
            request.unidadeTexto(),
            request.moradorSolicitante(),
            request.categoria(),
            request.assunto(),
            request.descricao()
        );
        return ResponseEntity.ok(ChamadoResponseDTO.fromEntity(chamado));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ChamadoResponseDTO> atualizarStatus(
            @PathVariable UUID id, 
            @Valid @RequestBody AtualizarStatusChamadoRequestDTO request) {
        
        Chamado.StatusChamado status = Chamado.StatusChamado.valueOf(request.novoStatus().toUpperCase());
        var chamado = atualizarStatusChamadoUseCase.executar(id, status);
        return ResponseEntity.ok(ChamadoResponseDTO.fromEntity(chamado));
    }
}

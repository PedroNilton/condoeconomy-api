package com.condoeconomy.api.presentation.controller.visitante;

import com.condoeconomy.api.application.usecase.visitante.AlterarStatusVisitanteUseCase;
import com.condoeconomy.api.application.usecase.visitante.CriarVisitanteUseCase;
import com.condoeconomy.api.application.usecase.visitante.ListarVisitantesUseCase;
import com.condoeconomy.api.presentation.dto.visitante.CriarVisitanteRequestDTO;
import com.condoeconomy.api.presentation.dto.visitante.VisitanteResponseDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/visitantes")
public class VisitanteController {

    private final CriarVisitanteUseCase criarVisitanteUseCase;
    private final ListarVisitantesUseCase listarVisitantesUseCase;
    private final AlterarStatusVisitanteUseCase alterarStatusVisitanteUseCase;

    public VisitanteController(CriarVisitanteUseCase criarVisitanteUseCase,
                               ListarVisitantesUseCase listarVisitantesUseCase,
                               AlterarStatusVisitanteUseCase alterarStatusVisitanteUseCase) {
        this.criarVisitanteUseCase = criarVisitanteUseCase;
        this.listarVisitantesUseCase = listarVisitantesUseCase;
        this.alterarStatusVisitanteUseCase = alterarStatusVisitanteUseCase;
    }

    @GetMapping
    public ResponseEntity<List<VisitanteResponseDTO>> listar(
            @RequestParam(required = false) String unidadeDestino,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVisita) {
        
        var visitantes = listarVisitantesUseCase.executar(unidadeDestino, dataVisita);
        var dtos = visitantes.stream().map(VisitanteResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<VisitanteResponseDTO> criar(@RequestBody CriarVisitanteRequestDTO request) {
        var visitante = criarVisitanteUseCase.executar(
                request.nome(),
                request.sobrenome(),
                request.blocoDestino(),
                request.unidadeDestino(),
                request.placaVeiculo()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(VisitanteResponseDTO.fromEntity(visitante));
    }

    @PutMapping("/{id}/checkin")
    public ResponseEntity<VisitanteResponseDTO> checkin(@PathVariable UUID id) {
        var visitante = alterarStatusVisitanteUseCase.registrarEntrada(id);
        return ResponseEntity.ok(VisitanteResponseDTO.fromEntity(visitante));
    }

    @PutMapping("/{id}/checkout")
    public ResponseEntity<VisitanteResponseDTO> checkout(@PathVariable UUID id) {
        var visitante = alterarStatusVisitanteUseCase.registrarSaida(id);
        return ResponseEntity.ok(VisitanteResponseDTO.fromEntity(visitante));
    }
}

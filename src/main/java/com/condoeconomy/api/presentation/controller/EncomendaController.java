package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.gateway.EncomendaRepository;
import com.condoeconomy.api.application.usecase.ReceberEncomendaUseCase;
import com.condoeconomy.api.presentation.dto.EncomendaResponseDTO;
import com.condoeconomy.api.presentation.dto.ReceberEncomendaRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/encomendas")
public class EncomendaController {

    private final ReceberEncomendaUseCase receberEncomendaUseCase;
    private final EncomendaRepository encomendaRepository;

    public EncomendaController(ReceberEncomendaUseCase receberEncomendaUseCase, EncomendaRepository encomendaRepository) {
        this.receberEncomendaUseCase = receberEncomendaUseCase;
        this.encomendaRepository = encomendaRepository;
    }

    @GetMapping
    public ResponseEntity<List<EncomendaResponseDTO>> listarEncomendas() {
        var encomendas = encomendaRepository.buscarTodas();
        var dtos = encomendas.stream().map(EncomendaResponseDTO::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<EncomendaResponseDTO> registrarChegada(
            @Valid @RequestBody ReceberEncomendaRequestDTO request) {
        
        var encomenda = receberEncomendaUseCase.executar(
                request.codigoRastreio(), 
                request.transportadora(), 
                request.destinatario(),
                request.unidade()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EncomendaResponseDTO.fromEntity(encomenda));
    }
}

package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.usecase.ReceberEncomendaUseCase;
import com.condoeconomy.api.presentation.dto.EncomendaResponseDTO;
import com.condoeconomy.api.presentation.dto.ReceberEncomendaRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/encomendas")
public class EncomendaController {

    private final ReceberEncomendaUseCase receberEncomendaUseCase;

    public EncomendaController(ReceberEncomendaUseCase receberEncomendaUseCase) {
        this.receberEncomendaUseCase = receberEncomendaUseCase;
    }

    @PostMapping
    public ResponseEntity<EncomendaResponseDTO> registrarChegada(
            @Valid @RequestBody ReceberEncomendaRequestDTO request) {
        
        var encomenda = receberEncomendaUseCase.executar(
                request.codigoRastreio(), 
                request.transportadora(), 
                request.unidadeId()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EncomendaResponseDTO.fromEntity(encomenda));
    }
}

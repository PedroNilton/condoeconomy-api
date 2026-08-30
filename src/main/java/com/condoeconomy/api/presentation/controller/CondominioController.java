package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.usecase.CadastrarCondominioUseCase;
import com.condoeconomy.api.presentation.dto.CondominioRequestDTO;
import com.condoeconomy.api.presentation.dto.CondominioResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/condominios")
public class CondominioController {

    private final CadastrarCondominioUseCase useCase;

    public CondominioController(CadastrarCondominioUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<CondominioResponseDTO> cadastrar(@Valid @RequestBody CondominioRequestDTO request) {
        var condominio = useCase.executar(request.nome(), request.cnpj(), request.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CondominioResponseDTO.fromEntity(condominio));
    }
}

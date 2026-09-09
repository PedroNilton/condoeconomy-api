package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.gateway.EncomendaRepository;
import com.condoeconomy.api.application.usecase.ReceberEncomendaUseCase;
import com.condoeconomy.api.application.usecase.RegistrarRetiradaUseCase;
import com.condoeconomy.api.presentation.dto.EncomendaResponseDTO;
import com.condoeconomy.api.presentation.dto.ReceberEncomendaRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/encomendas")
public class EncomendaController {

    private final ReceberEncomendaUseCase receberEncomendaUseCase;
    private final RegistrarRetiradaUseCase registrarRetiradaUseCase;
    private final EncomendaRepository encomendaRepository;

    public EncomendaController(ReceberEncomendaUseCase receberEncomendaUseCase, 
                               RegistrarRetiradaUseCase registrarRetiradaUseCase,
                               EncomendaRepository encomendaRepository) {
        this.receberEncomendaUseCase = receberEncomendaUseCase;
        this.registrarRetiradaUseCase = registrarRetiradaUseCase;
        this.encomendaRepository = encomendaRepository;
    }

    @GetMapping
    public ResponseEntity<List<EncomendaResponseDTO>> listarEncomendas() {
        var encomendas = encomendaRepository.buscarTodas();
        var dtos = encomendas.stream().map(EncomendaResponseDTO::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<EncomendaResponseDTO>> listarMinhasEncomendas(org.springframework.security.core.Authentication auth) {
        com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity user = 
            (com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity) auth.getPrincipal();
        
        var encomendas = encomendaRepository.buscarPorMorador(user.getNome(), user.getBloco(), user.getApartamento());
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

    @PutMapping("/{id}/retirar")
    public ResponseEntity<EncomendaResponseDTO> registrarRetirada(@PathVariable UUID id) {
        var encomenda = registrarRetiradaUseCase.executar(id);
        return ResponseEntity.ok(EncomendaResponseDTO.fromEntity(encomenda));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<java.util.Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", ex.getMessage()));
    }
}

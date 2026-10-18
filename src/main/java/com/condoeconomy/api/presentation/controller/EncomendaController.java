package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.gateway.EncomendaRepository;
import com.condoeconomy.api.application.usecase.ReceberEncomendaUseCase;
import com.condoeconomy.api.application.usecase.RegistrarRetiradaUseCase;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.presentation.dto.EncomendaResponseDTO;
import com.condoeconomy.api.presentation.dto.ReceberEncomendaRequestDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/encomendas")
public class EncomendaController {

    private static final Logger log = LoggerFactory.getLogger(EncomendaController.class);

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
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<EncomendaResponseDTO>> listarEncomendas() {
        var encomendas = encomendaRepository.buscarTodas();
        var dtos = encomendas.stream().map(EncomendaResponseDTO::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<EncomendaResponseDTO>> listarMinhasEncomendas(Authentication auth) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        
        var encomendas = encomendaRepository.buscarPorMorador(user.getNome(), user.getBloco(), user.getApartamento());
        var dtos = encomendas.stream().map(EncomendaResponseDTO::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<EncomendaResponseDTO> registrarChegada(
            @Valid @RequestBody ReceberEncomendaRequestDTO request,
            Authentication auth) {
        SecuritySanitizer.validarTextoSeguro(request.codigoRastreio(), "codigoRastreio");
        SecuritySanitizer.validarTextoSeguro(request.transportadora(), "transportadora");
        SecuritySanitizer.validarTextoSeguro(request.destinatario(), "destinatario");
        SecuritySanitizer.validarTextoSeguro(request.unidade(), "unidade");

        var encomenda = receberEncomendaUseCase.executar(
                SecuritySanitizer.sanitizar(request.codigoRastreio()), 
                SecuritySanitizer.sanitizar(request.transportadora()), 
                SecuritySanitizer.sanitizar(request.destinatario()),
                SecuritySanitizer.sanitizar(request.unidade())
        );

        String operador = auth != null ? auth.getName() : "SISTEMA";
        log.info("[AUDITORIA DE PORTARIA] Encomenda código '{}' registrada para '{}' ({}) pelo operador '{}'",
                encomenda.getCodigoRastreio(), encomenda.getDestinatario(), encomenda.getUnidade(), operador);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EncomendaResponseDTO.fromEntity(encomenda));
    }

    @PutMapping("/{id}/retirar")
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<EncomendaResponseDTO> registrarRetirada(@PathVariable UUID id, Authentication auth) {
        var encomenda = registrarRetiradaUseCase.executar(id);
        String operador = auth != null ? auth.getName() : "SISTEMA";
        log.info("[AUDITORIA DE PORTARIA] Retirada da encomenda ID={} registrada pelo operador '{}'", id, operador);
        return ResponseEntity.ok(EncomendaResponseDTO.fromEntity(encomenda));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<java.util.Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("message", ex.getMessage()));
    }
}

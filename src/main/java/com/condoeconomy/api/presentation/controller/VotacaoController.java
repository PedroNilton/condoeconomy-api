package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.service.VotacaoService;
import com.condoeconomy.api.presentation.dto.votacao.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/votacoes")
@RequiredArgsConstructor
public class VotacaoController {

    private final VotacaoService votacaoService;

    // Morador: Listar ativas
    @GetMapping("/ativas")
    public ResponseEntity<List<VotacaoResponseDTO>> listarAtivas(Authentication authentication) {
        return ResponseEntity.ok(votacaoService.listarAtivas(authentication.getName()));
    }

    // Admin/Síndico: Listar todas
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<VotacaoResponseDTO>> listarTodas(Authentication authentication) {
        return ResponseEntity.ok(votacaoService.listarTodas(authentication.getName()));
    }

    // Morador: Votar
    @PostMapping("/{id}/votar")
    @PreAuthorize("hasRole('ROLE_MORADOR')")
    public ResponseEntity<Void> votar(@PathVariable UUID id, @RequestBody @Valid VotoRequestDTO dto, Authentication authentication) {
        votacaoService.registrarVoto(id, dto, authentication.getName());
        return ResponseEntity.ok().build();
    }

    // Ambos: Ver resultado
    @GetMapping("/{id}/resultado")
    public ResponseEntity<VotacaoResultadoDTO> obterResultado(@PathVariable UUID id) {
        return ResponseEntity.ok(votacaoService.obterResultado(id));
    }

    // Admin/Síndico: Criar
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<VotacaoResponseDTO> criar(@RequestBody @Valid VotacaoRequestDTO dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(votacaoService.criarVotacao(dto, authentication.getName()));
    }

    // Admin/Síndico: Encerrar
    @PutMapping("/{id}/encerrar")
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> encerrar(@PathVariable UUID id) {
        votacaoService.encerrarVotacao(id);
        return ResponseEntity.noContent().build();
    }
}

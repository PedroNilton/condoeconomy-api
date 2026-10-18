package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.service.VotacaoService;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.presentation.dto.votacao.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/votacoes")
@RequiredArgsConstructor
public class VotacaoController {

    private static final Logger log = LoggerFactory.getLogger(VotacaoController.class);

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
        log.info("[AUDITORIA DE ASSEMBLEIA] Voto registrado na pauta ID={} pelo morador '{}'", id, authentication.getName());
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
        SecuritySanitizer.validarTextoSeguro(dto.getTitulo(), "titulo");
        SecuritySanitizer.validarTextoSeguro(dto.getDescricao(), "descricao");
        dto.setTitulo(SecuritySanitizer.sanitizar(dto.getTitulo()));
        if (dto.getDescricao() != null) {
            dto.setDescricao(SecuritySanitizer.sanitizar(dto.getDescricao()));
        }
        if (dto.getOpcoes() != null) {
            dto.setOpcoes(dto.getOpcoes().stream().map(op -> {
                SecuritySanitizer.validarTextoSeguro(op, "opcao");
                return SecuritySanitizer.sanitizar(op);
            }).collect(Collectors.toList()));
        }

        VotacaoResponseDTO criada = votacaoService.criarVotacao(dto, authentication.getName());
        log.info("[AUDITORIA DE ASSEMBLEIA] Nova pauta '{}' criada pelo usuário '{}'", dto.getTitulo(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    // Admin/Síndico: Encerrar
    @PutMapping("/{id}/encerrar")
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> encerrar(@PathVariable UUID id, Authentication authentication) {
        votacaoService.encerrarVotacao(id);
        String operador = authentication != null ? authentication.getName() : "SISTEMA";
        log.info("[AUDITORIA DE ASSEMBLEIA] Pauta ID={} encerrada pelo operador '{}'", id, operador);
        return ResponseEntity.noContent().build();
    }
}

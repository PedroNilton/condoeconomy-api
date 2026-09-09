package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.domain.entity.notificacao.Notificacao;
import com.condoeconomy.api.infrastructure.repository.notificacao.NotificacaoRepository;
import com.condoeconomy.api.presentation.dto.notificacao.NotificacaoResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notificacoes")
public class NotificacaoController {

    private final NotificacaoRepository repository;

    public NotificacaoController(NotificacaoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<NotificacaoResponseDTO>> listarNotificacoes() {
        UsuarioJpaEntity usuario = getUsuarioAutenticado();
        List<NotificacaoResponseDTO> lista = repository.findByUsuarioIdOrderByDataCriacaoDesc(usuario.getId())
            .stream()
            .map(n -> new NotificacaoResponseDTO(n.getId(), n.getTitulo(), n.getMensagem(), n.isLida(), n.getDataCriacao()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/nao-lidas/count")
    public ResponseEntity<Long> contarNaoLidas() {
        UsuarioJpaEntity usuario = getUsuarioAutenticado();
        return ResponseEntity.ok(repository.countByUsuarioIdAndLidaFalse(usuario.getId()));
    }

    @PutMapping("/{id}/lida")
    public ResponseEntity<Void> marcarComoLida(@PathVariable UUID id) {
        repository.findById(id).ifPresent(n -> {
            n.marcarComoLida();
            repository.save(n);
        });
        return ResponseEntity.ok().build();
    }

    private UsuarioJpaEntity getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (UsuarioJpaEntity) auth.getPrincipal();
    }
}

package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.usecase.CriarPedidoUseCase;
import com.condoeconomy.api.domain.entity.Pedido;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.presentation.dto.CriarPedidoRequestDTO;
import com.condoeconomy.api.presentation.dto.PedidoResponseDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/condostore/pedidos")
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    private final CriarPedidoUseCase criarPedidoUseCase;

    public PedidoController(CriarPedidoUseCase criarPedidoUseCase) {
        this.criarPedidoUseCase = criarPedidoUseCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_MORADOR')")
    public ResponseEntity<PedidoResponseDTO> criarPedido(
            @Valid @RequestBody CriarPedidoRequestDTO request,
            Authentication auth) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        UUID moradorAutenticadoId = user.getId();

        if (request.moradorId() != null && !request.moradorId().equals(moradorAutenticadoId)) {
            log.warn("[AUDITORIA DE SEGURANÇA] Tentativa de falsidade ideológica no CondoStore: usuário '{}' (ID={}) tentou criar pedido em nome de ID={}",
                    user.getEmail(), moradorAutenticadoId, request.moradorId());
        }

        Pedido pedido = criarPedidoUseCase.executar(moradorAutenticadoId, request.itens());
        log.info("[AUDITORIA DE CONDOSTORE] Pedido ID={} registrado para o morador '{}' (ID={})",
                pedido.getId(), user.getEmail(), moradorAutenticadoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponseDTO.fromEntity(pedido));
    }
}

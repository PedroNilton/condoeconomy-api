package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.usecase.CriarPedidoUseCase;
import com.condoeconomy.api.domain.entity.Pedido;
import com.condoeconomy.api.presentation.dto.CriarPedidoRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/condostore/pedidos")
public class PedidoController {

    private final CriarPedidoUseCase criarPedidoUseCase;

    public PedidoController(CriarPedidoUseCase criarPedidoUseCase) {
        this.criarPedidoUseCase = criarPedidoUseCase;
    }

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criarPedido(@Valid @RequestBody CriarPedidoRequestDTO request) {
        Pedido pedido = criarPedidoUseCase.executar(request.moradorId(), request.itens());
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponseDTO.fromEntity(pedido));
    }
}

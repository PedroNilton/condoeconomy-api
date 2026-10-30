package com.condoeconomy.api.presentation.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Map;
import java.util.UUID;

public record CriarPedidoRequestDTO(
        UUID moradorId,
        @NotEmpty(message = "O pedido deve ter pelo menos um item") Map<UUID, Integer> itens
) {}

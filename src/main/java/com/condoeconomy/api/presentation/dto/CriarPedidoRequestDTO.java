package com.condoeconomy.api.presentation.dto;

import java.util.Map;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CriarPedidoRequestDTO(
        @NotNull(message = "O ID do morador é obrigatório") UUID moradorId,
        @NotEmpty(message = "O pedido deve ter pelo menos um item") Map<UUID, Integer> itens
) {}

package com.condoeconomy.api.presentation.dto;

import com.condoeconomy.api.domain.entity.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record PedidoResponseDTO(
        UUID id,
        UUID moradorId,
        String status,
        LocalDateTime dataCriacao,
        BigDecimal total,
        List<ItemPedidoResponseDTO> itens
) {
    public static PedidoResponseDTO fromEntity(Pedido p) {
        List<ItemPedidoResponseDTO> itensDto = p.getItens().stream()
                .map(i -> new ItemPedidoResponseDTO(i.getProdutoId(), i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal()))
                .collect(Collectors.toList());
                
        return new PedidoResponseDTO(
                p.getId(),
                p.getMoradorId(),
                p.getStatus().name(),
                p.getDataCriacao(),
                p.calcularTotal(),
                itensDto
        );
    }
}

record ItemPedidoResponseDTO(
        UUID produtoId,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {}

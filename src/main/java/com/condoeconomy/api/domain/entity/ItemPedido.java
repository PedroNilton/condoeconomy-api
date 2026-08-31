package com.condoeconomy.api.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class ItemPedido {
    private final UUID produtoId;
    private final int quantidade;
    private final BigDecimal precoUnitario;
    private final BigDecimal subtotal;

    public ItemPedido(UUID produtoId, int quantidade, BigDecimal precoUnitario, BigDecimal subtotal) {
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = subtotal;
    }

    public UUID getProdutoId() { return produtoId; }
    public int getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
}

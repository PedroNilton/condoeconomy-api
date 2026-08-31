package com.condoeconomy.api.domain.entity;

import com.condoeconomy.api.domain.enums.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Pedido {
    private final UUID id;
    private final UUID moradorId;
    private StatusPedido status;
    private final LocalDateTime dataCriacao;
    private final List<ItemPedido> itens;

    public Pedido(UUID id, UUID moradorId) {
        if (moradorId == null) {
            throw new IllegalArgumentException("O pedido deve estar associado a um morador");
        }
        this.id = id;
        this.moradorId = moradorId;
        this.status = StatusPedido.CRIADO;
        this.dataCriacao = LocalDateTime.now();
        this.itens = new ArrayList<>();
    }

    public void adicionarItem(Produto produto, int quantidade) {
        if (this.status != StatusPedido.CRIADO) {
            throw new IllegalStateException("Não é possível adicionar itens a um pedido já fechado.");
        }
        produto.decrementarEstoque(quantidade);
        
        BigDecimal subtotal = produto.getPreco().multiply(BigDecimal.valueOf(quantidade));
        this.itens.add(new ItemPedido(produto.getId(), quantidade, produto.getPreco(), subtotal));
    }

    public BigDecimal calcularTotal() {
        return itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public void marcarComoPago() {
        if (this.status != StatusPedido.CRIADO) {
            throw new IllegalStateException("Só é possível pagar um pedido CRIADO.");
        }
        this.status = StatusPedido.PAGO;
    }

    public UUID getId() { return id; }
    public UUID getMoradorId() { return moradorId; }
    public StatusPedido getStatus() { return status; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public List<ItemPedido> getItens() { return Collections.unmodifiableList(itens); }
}

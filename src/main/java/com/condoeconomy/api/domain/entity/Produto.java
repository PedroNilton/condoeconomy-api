package com.condoeconomy.api.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

public class Produto {
    private final UUID id;
    private final String nome;
    private final String descricao;
    private BigDecimal preco;
    private int quantidadeEstoque;

    public Produto(UUID id, String nome, String descricao, BigDecimal preco, int quantidadeEstoque) {
        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço não pode ser negativo");
        }
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public void decrementarEstoque(int quantidade) {
        if (quantidade <= 0) throw new IllegalArgumentException("Quantidade inválida");
        if (this.quantidadeEstoque < quantidade) {
            throw new IllegalStateException("Estoque insuficiente para o produto: " + this.nome);
        }
        this.quantidadeEstoque -= quantidade;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPreco() { return preco; }
    public int getQuantidadeEstoque() { return quantidadeEstoque; }
}

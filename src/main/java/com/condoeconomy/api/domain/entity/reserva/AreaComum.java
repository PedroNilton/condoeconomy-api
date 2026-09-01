package com.condoeconomy.api.domain.entity.reserva;

import java.math.BigDecimal;
import java.util.UUID;

public class AreaComum {
    private UUID id;
    private String nome;
    private String descricao;
    private Integer capacidadeMaxima;
    private BigDecimal taxaLocacao;

    public AreaComum(UUID id, String nome, String descricao, Integer capacidadeMaxima, BigDecimal taxaLocacao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.capacidadeMaxima = capacidadeMaxima;
        this.taxaLocacao = taxaLocacao;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public Integer getCapacidadeMaxima() { return capacidadeMaxima; }
    public BigDecimal getTaxaLocacao() { return taxaLocacao; }
}

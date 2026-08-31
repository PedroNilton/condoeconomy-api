package com.condoeconomy.api.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "produto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoJpaEntity {
    @Id
    private UUID id;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private int quantidadeEstoque;
}

package com.condoeconomy.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "item_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedidoJpaEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "produto_id")
    private UUID produtoId; 
    
    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private PedidoJpaEntity pedido;
    
    private int quantidade;
    
    @Column(name = "preco_unitario")
    private BigDecimal precoUnitario;
    
    private BigDecimal subtotal;
}

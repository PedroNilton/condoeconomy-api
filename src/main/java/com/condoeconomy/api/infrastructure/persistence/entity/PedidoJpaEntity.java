package com.condoeconomy.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoJpaEntity {
    @Id
    private UUID id;
    
    @Column(name = "morador_id")
    private UUID moradorId;
    
    private String status;
    
    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;
    
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL)
    private List<ItemPedidoJpaEntity> itens;
}

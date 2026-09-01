package com.condoeconomy.api.infrastructure.persistence.entity.reserva;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "area_comum")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaComumJpaEntity {
    @Id
    private UUID id;
    private String nome;
    private String descricao;
    private Integer capacidadeMaxima;
    private BigDecimal taxaLocacao;
}

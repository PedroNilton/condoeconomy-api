package com.condoeconomy.api.infrastructure.persistence.entity.financeiro;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "despesa")
@Data
public class DespesaJpaEntity {
    @Id
    private UUID id;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private String status; // PENDENTE, PAGO
}

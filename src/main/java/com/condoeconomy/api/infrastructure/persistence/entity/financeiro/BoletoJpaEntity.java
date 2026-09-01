package com.condoeconomy.api.infrastructure.persistence.entity.financeiro;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "boleto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoletoJpaEntity {
    @Id
    private UUID id;
    private String unidadeTexto;
    private String moradorResponsavel;
    private String competencia;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private String status;
    private String linhaDigitavel;
    private String urlPdf;
}

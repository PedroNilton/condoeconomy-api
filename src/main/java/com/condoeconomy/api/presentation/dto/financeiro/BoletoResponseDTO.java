package com.condoeconomy.api.presentation.dto.financeiro;

import com.condoeconomy.api.domain.entity.financeiro.Boleto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BoletoResponseDTO(
    UUID id,
    String unidadeTexto,
    String moradorResponsavel,
    String competencia,
    BigDecimal valor,
    LocalDate dataVencimento,
    LocalDate dataPagamento,
    String status,
    String linhaDigitavel,
    String urlPdf
) {
    public static BoletoResponseDTO fromEntity(Boleto b) {
        return new BoletoResponseDTO(
            b.getId(), b.getUnidadeTexto(), b.getMoradorResponsavel(), b.getCompetencia(),
            b.getValor(), b.getDataVencimento(), b.getDataPagamento(), 
            b.getStatus().name(), b.getLinhaDigitavel(), b.getUrlPdf()
        );
    }
}

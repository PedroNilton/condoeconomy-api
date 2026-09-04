package com.condoeconomy.api.presentation.dto.financeiro;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardFinanceiroResponse {
    private BigDecimal receitaPrevista;
    private BigDecimal receitaArrecadada;
    private BigDecimal despesas;
    private Double inadimplencia;
    private Long unidadesInadimplentes;
    private List<BoletoResumo> boletosRecentes;

    @Data
    @Builder
    public static class BoletoResumo {
        private String id;
        private String unidade;
        private BigDecimal valor;
        private String status;
        private String data;
    }
}

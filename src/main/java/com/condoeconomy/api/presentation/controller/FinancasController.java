package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataBoletoRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataDespesaRepository;
import com.condoeconomy.api.presentation.dto.financeiro.DashboardFinanceiroResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/financas")
@RequiredArgsConstructor
public class FinancasController {

    private final SpringDataBoletoRepository boletoRepository;
    private final SpringDataDespesaRepository despesaRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('SINDICO')")
    public ResponseEntity<DashboardFinanceiroResponse> getDashboard() {
        
        BigDecimal totalEsperado = boletoRepository.sumTotalEsperado();
        BigDecimal totalPago = boletoRepository.sumTotalPago();
        BigDecimal totalDespesas = despesaRepository.sumTotalDespesas();
        
        Long inadimplentes = boletoRepository.countUnidadesInadimplentes();
        Long totalUnidades = boletoRepository.countTotalUnidades();
        
        if (totalEsperado == null) totalEsperado = BigDecimal.ZERO;
        if (totalPago == null) totalPago = BigDecimal.ZERO;
        if (totalDespesas == null) totalDespesas = BigDecimal.ZERO;
        if (inadimplentes == null) inadimplentes = 0L;
        if (totalUnidades == null || totalUnidades == 0) totalUnidades = 1L;

        double inadimplenciaPct = ((double) inadimplentes / totalUnidades) * 100.0;
        inadimplenciaPct = Math.round(inadimplenciaPct * 10.0) / 10.0; // 1 decimal place

        var boletos = boletoRepository.findTop5ByOrderByDataVencimentoDesc().stream().map(b -> 
            DashboardFinanceiroResponse.BoletoResumo.builder()
                .id(b.getId().toString())
                .unidade(b.getUnidadeTexto())
                .valor(b.getValor())
                .status(b.getStatus())
                .data(b.getDataVencimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .build()
        ).collect(Collectors.toList());

        DashboardFinanceiroResponse response = DashboardFinanceiroResponse.builder()
                .receitaPrevista(totalEsperado)
                .receitaArrecadada(totalPago)
                .despesas(totalDespesas)
                .unidadesInadimplentes(inadimplentes)
                .inadimplencia(inadimplenciaPct)
                .boletosRecentes(boletos)
                .build();

        return ResponseEntity.ok(response);
    }
}

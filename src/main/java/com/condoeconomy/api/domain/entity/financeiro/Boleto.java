package com.condoeconomy.api.domain.entity.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class Boleto {
    private UUID id;
    private String unidadeTexto;
    private String moradorResponsavel;
    private String competencia;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private StatusBoleto status;
    private String linhaDigitavel;
    private String urlPdf;

    public enum StatusBoleto {
        PENDENTE, PAGO, VENCIDO
    }

    public Boleto(UUID id, String unidadeTexto, String moradorResponsavel, String competencia, 
                  BigDecimal valor, LocalDate dataVencimento, LocalDate dataPagamento, 
                  StatusBoleto status, String linhaDigitavel, String urlPdf) {
        this.id = id;
        this.unidadeTexto = unidadeTexto;
        this.moradorResponsavel = moradorResponsavel;
        this.competencia = competencia;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
        this.status = status;
        this.linhaDigitavel = linhaDigitavel;
        this.urlPdf = urlPdf;
    }

    public void atualizarStatus() {
        if (this.status == StatusBoleto.PAGO) return;
        if (LocalDate.now().isAfter(dataVencimento)) {
            this.status = StatusBoleto.VENCIDO;
        } else {
            this.status = StatusBoleto.PENDENTE;
        }
    }

    public void registrarPagamento(LocalDate dataPagamento) {
        this.status = StatusBoleto.PAGO;
        this.dataPagamento = dataPagamento;
    }

    // Getters
    public UUID getId() { return id; }
    public String getUnidadeTexto() { return unidadeTexto; }
    public String getMoradorResponsavel() { return moradorResponsavel; }
    public String getCompetencia() { return competencia; }
    public BigDecimal getValor() { return valor; }
    public LocalDate getDataVencimento() { return dataVencimento; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public StatusBoleto getStatus() { return status; }
    public String getLinhaDigitavel() { return linhaDigitavel; }
    public String getUrlPdf() { return urlPdf; }
}

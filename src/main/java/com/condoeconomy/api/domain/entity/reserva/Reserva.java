package com.condoeconomy.api.domain.entity.reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Reserva {
    private UUID id;
    private AreaComum areaComum;
    private String unidadeTexto;
    private String moradorSolicitante;
    private String titulo;
    private LocalDate dataReserva;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private StatusReserva status;
    private LocalDateTime dataSolicitacao;
    private List<Convidado> convidados;

    public enum StatusReserva {
        APROVADA, CANCELADA, REALIZADA
    }

    public Reserva(UUID id, AreaComum areaComum, String unidadeTexto, String moradorSolicitante, 
                   String titulo, LocalDate dataReserva, LocalTime horaInicio, LocalTime horaFim, 
                   StatusReserva status, LocalDateTime dataSolicitacao, List<Convidado> convidados) {
        this.id = id;
        this.areaComum = areaComum;
        this.unidadeTexto = unidadeTexto;
        this.moradorSolicitante = moradorSolicitante;
        this.titulo = titulo;
        this.dataReserva = dataReserva;
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.status = status;
        this.dataSolicitacao = dataSolicitacao;
        this.convidados = convidados != null ? convidados : new ArrayList<>();
    }

    public static Reserva criar(AreaComum area, String unidade, String morador, String titulo, 
                                LocalDate data, LocalTime inicio, LocalTime fim, List<Convidado> convidados) {
        return new Reserva(UUID.randomUUID(), area, unidade, morador, titulo, data, inicio, fim, 
                           StatusReserva.APROVADA, LocalDateTime.now(), convidados);
    }

    public void cancelar() {
        this.status = StatusReserva.CANCELADA;
    }

    // Getters
    public UUID getId() { return id; }
    public AreaComum getAreaComum() { return areaComum; }
    public String getUnidadeTexto() { return unidadeTexto; }
    public String getMoradorSolicitante() { return moradorSolicitante; }
    public String getTitulo() { return titulo; }
    public LocalDate getDataReserva() { return dataReserva; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public StatusReserva getStatus() { return status; }
    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public List<Convidado> getConvidados() { return convidados; }
}

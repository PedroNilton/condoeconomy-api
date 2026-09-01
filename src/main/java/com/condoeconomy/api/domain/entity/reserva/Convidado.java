package com.condoeconomy.api.domain.entity.reserva;

import java.time.LocalDateTime;
import java.util.UUID;

public class Convidado {
    private UUID id;
    private String nome;
    private String documento;
    private StatusEntrada statusEntrada;
    private LocalDateTime horaEntrada;

    public enum StatusEntrada {
        PENDENTE, ENTROU
    }

    public Convidado(UUID id, String nome, String documento, StatusEntrada statusEntrada, LocalDateTime horaEntrada) {
        this.id = id;
        this.nome = nome;
        this.documento = documento;
        this.statusEntrada = statusEntrada;
        this.horaEntrada = horaEntrada;
    }

    public static Convidado criar(String nome, String documento) {
        return new Convidado(UUID.randomUUID(), nome, documento, StatusEntrada.PENDENTE, null);
    }

    public void registrarEntrada() {
        this.statusEntrada = StatusEntrada.ENTROU;
        this.horaEntrada = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDocumento() { return documento; }
    public StatusEntrada getStatusEntrada() { return statusEntrada; }
    public LocalDateTime getHoraEntrada() { return horaEntrada; }
}

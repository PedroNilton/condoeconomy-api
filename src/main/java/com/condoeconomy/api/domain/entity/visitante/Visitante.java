package com.condoeconomy.api.domain.entity.visitante;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "visitante")
public class Visitante {

    @Id
    private UUID id;
    private String nome;
    private String documento;
    private LocalDate dataVisita;
    private String unidadeDestino;
    private String moradorResponsavel;
    private String tipo; // VISITANTE, PRESTADOR_SERVICO
    private String status; // AGUARDANDO, NO_CONDOMINIO, FINALIZADO
    private LocalDateTime horaEntrada;
    private LocalDateTime horaSaida;

    public Visitante() {
    }

    public Visitante(String nome, String documento, LocalDate dataVisita, String unidadeDestino, String moradorResponsavel, String tipo) {
        this.id = UUID.randomUUID();
        this.nome = nome;
        this.documento = documento;
        this.dataVisita = dataVisita;
        this.unidadeDestino = unidadeDestino;
        this.moradorResponsavel = moradorResponsavel;
        this.tipo = tipo;
        this.status = "AGUARDANDO";
    }

    public void registrarEntrada() {
        if (this.status.equals("FINALIZADO")) {
            throw new IllegalStateException("Esta visita já foi finalizada.");
        }
        this.status = "NO_CONDOMINIO";
        this.horaEntrada = LocalDateTime.now();
    }

    public void registrarSaida() {
        if (!this.status.equals("NO_CONDOMINIO")) {
            throw new IllegalStateException("O visitante não está no condomínio.");
        }
        this.status = "FINALIZADO";
        this.horaSaida = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getDocumento() { return documento; }
    public LocalDate getDataVisita() { return dataVisita; }
    public String getUnidadeDestino() { return unidadeDestino; }
    public String getMoradorResponsavel() { return moradorResponsavel; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
    public LocalDateTime getHoraEntrada() { return horaEntrada; }
    public LocalDateTime getHoraSaida() { return horaSaida; }
}

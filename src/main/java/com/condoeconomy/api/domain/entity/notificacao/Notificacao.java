package com.condoeconomy.api.domain.entity.notificacao;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notificacao")
public class Notificacao {
    @Id
    private UUID id;
    private String titulo;
    private String mensagem;
    private boolean lida;
    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;
    @Column(name = "usuario_id")
    private UUID usuarioId;
    private String categoria = "GERAL";

    public Notificacao() {}

    public Notificacao(UUID id, String titulo, String mensagem, UUID usuarioId) {
        this(id, titulo, mensagem, usuarioId, "GERAL");
    }

    public Notificacao(UUID id, String titulo, String mensagem, UUID usuarioId, String categoria) {
        this.id = id;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.lida = false;
        this.dataCriacao = LocalDateTime.now();
        this.usuarioId = usuarioId;
        this.categoria = (categoria != null && !categoria.isBlank()) ? categoria : "GERAL";
    }

    public UUID getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public boolean isLida() { return lida; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getCategoria() { return categoria != null ? categoria : "GERAL"; }

    public void marcarComoLida() {
        this.lida = true;
    }
}

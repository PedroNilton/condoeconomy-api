package com.condoeconomy.api.domain.entity.aviso;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "aviso")
public class Aviso {

    @Id
    private String id;
    private String titulo;
    private String mensagem;
    private LocalDateTime dataCriacao;
    private String autor;
    private String categoria = "Aviso geral";
    private String blocos;
    private boolean fixado = false;
    private int visualizacoes = 0;

    public Aviso() {}

    public Aviso(String id, String titulo, String mensagem, LocalDateTime dataCriacao, String autor) {
        this(id, titulo, mensagem, dataCriacao, autor, "Aviso geral", null, false, 0);
    }

    public Aviso(String id, String titulo, String mensagem, LocalDateTime dataCriacao, String autor,
                 String categoria, String blocos, boolean fixado, int visualizacoes) {
        this.id = id;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.dataCriacao = dataCriacao;
        this.autor = autor;
        this.categoria = (categoria != null && !categoria.isBlank()) ? categoria : "Aviso geral";
        this.blocos = blocos;
        this.fixado = fixado;
        this.visualizacoes = visualizacoes;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public String getAutor() { return autor; }
    public String getCategoria() { return categoria != null ? categoria : "Aviso geral"; }
    public String getBlocos() { return blocos; }
    public boolean isFixado() { return fixado; }
    public int getVisualizacoes() { return visualizacoes; }

    public void alternarFixado() {
        this.fixado = !this.fixado;
    }

    public void registrarVisualizacao() {
        this.visualizacoes += 1;
    }
}

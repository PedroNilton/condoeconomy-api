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

    public Aviso() {}

    public Aviso(String id, String titulo, String mensagem, LocalDateTime dataCriacao, String autor) {
        this.id = id;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.dataCriacao = dataCriacao;
        this.autor = autor;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public String getAutor() { return autor; }
}

package com.condoeconomy.api.domain.entity.chamado;

import java.time.LocalDateTime;
import java.util.UUID;

public class Chamado {
    private UUID id;
    private String unidadeTexto;
    private String moradorSolicitante;
    private CategoriaChamado categoria;
    private String assunto;
    private String descricao;
    private StatusChamado status;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataResolucao;

    public enum StatusChamado { ABERTO, EM_ANDAMENTO, RESOLVIDO }
    public enum CategoriaChamado { RECLAMACAO, SUGESTAO, MANUTENCAO, OUTROS }

    public Chamado(UUID id, String unidadeTexto, String moradorSolicitante, CategoriaChamado categoria, 
                   String assunto, String descricao, StatusChamado status, 
                   LocalDateTime dataAbertura, LocalDateTime dataResolucao) {
        this.id = id;
        this.unidadeTexto = unidadeTexto;
        this.moradorSolicitante = moradorSolicitante;
        this.categoria = categoria;
        this.assunto = assunto;
        this.descricao = descricao;
        this.status = status;
        this.dataAbertura = dataAbertura;
        this.dataResolucao = dataResolucao;
    }

    public static Chamado abrir(String unidadeTexto, String moradorSolicitante, 
                                CategoriaChamado categoria, String assunto, String descricao) {
        return new Chamado(UUID.randomUUID(), unidadeTexto, moradorSolicitante, categoria, 
                           assunto, descricao, StatusChamado.ABERTO, LocalDateTime.now(), null);
    }

    public void iniciarAtendimento() {
        if (this.status == StatusChamado.RESOLVIDO) {
            throw new IllegalStateException("Chamado já está resolvido");
        }
        this.status = StatusChamado.EM_ANDAMENTO;
    }

    public void resolver() {
        this.status = StatusChamado.RESOLVIDO;
        this.dataResolucao = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public String getUnidadeTexto() { return unidadeTexto; }
    public String getMoradorSolicitante() { return moradorSolicitante; }
    public CategoriaChamado getCategoria() { return categoria; }
    public String getAssunto() { return assunto; }
    public String getDescricao() { return descricao; }
    public StatusChamado getStatus() { return status; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public LocalDateTime getDataResolucao() { return dataResolucao; }
}

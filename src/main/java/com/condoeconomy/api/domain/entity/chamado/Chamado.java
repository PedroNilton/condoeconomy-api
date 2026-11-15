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
    private boolean escaladoSindico;
    private String fotoAnexo;
    private String notaPortaria;

    public enum StatusChamado { ABERTO, EM_ANDAMENTO, RESOLVIDO }
    public enum CategoriaChamado { RECLAMACAO, SUGESTAO, MANUTENCAO, DUVIDA, SEGURANCA, ELOGIO, OUTROS }

    public Chamado() {} // Hibernate

    public Chamado(UUID id, String unidadeTexto, String moradorSolicitante, CategoriaChamado categoria, 
                   String assunto, String descricao, StatusChamado status, 
                   LocalDateTime dataAbertura, LocalDateTime dataResolucao, boolean escaladoSindico) {
        this(id, unidadeTexto, moradorSolicitante, categoria, assunto, descricao, status, dataAbertura, dataResolucao, escaladoSindico, null, null);
    }

    public Chamado(UUID id, String unidadeTexto, String moradorSolicitante, CategoriaChamado categoria, 
                   String assunto, String descricao, StatusChamado status, 
                   LocalDateTime dataAbertura, LocalDateTime dataResolucao, boolean escaladoSindico, String fotoAnexo) {
        this(id, unidadeTexto, moradorSolicitante, categoria, assunto, descricao, status, dataAbertura, dataResolucao, escaladoSindico, fotoAnexo, null);
    }

    public Chamado(UUID id, String unidadeTexto, String moradorSolicitante, CategoriaChamado categoria, 
                   String assunto, String descricao, StatusChamado status, 
                   LocalDateTime dataAbertura, LocalDateTime dataResolucao, boolean escaladoSindico, String fotoAnexo, String notaPortaria) {
        this.id = id;
        this.unidadeTexto = unidadeTexto;
        this.moradorSolicitante = moradorSolicitante;
        this.categoria = categoria;
        this.assunto = assunto;
        this.descricao = descricao;
        this.status = status;
        this.dataAbertura = dataAbertura;
        this.dataResolucao = dataResolucao;
        this.escaladoSindico = escaladoSindico;
        this.fotoAnexo = fotoAnexo;
        this.notaPortaria = notaPortaria;
    }

    public static Chamado abrir(String unidadeTexto, String moradorSolicitante, 
                                CategoriaChamado categoria, String assunto, String descricao) {
        return abrir(unidadeTexto, moradorSolicitante, categoria, assunto, descricao, null);
    }

    public static Chamado abrir(String unidadeTexto, String moradorSolicitante, 
                                CategoriaChamado categoria, String assunto, String descricao, String fotoAnexo) {
        return new Chamado(UUID.randomUUID(), unidadeTexto, moradorSolicitante, categoria, 
                           assunto, descricao, StatusChamado.ABERTO, LocalDateTime.now(), null, false, fotoAnexo, null);
    }

    public void iniciarAtendimento() {
        this.status = StatusChamado.EM_ANDAMENTO;
        this.dataResolucao = null;
    }

    public void resolver() {
        this.status = StatusChamado.RESOLVIDO;
        this.dataResolucao = LocalDateTime.now();
    }

    public void escalarParaSindico() {
        escalarParaSindico(null);
    }

    public void escalarParaSindico(String notaPortaria) {
        this.escaladoSindico = true;
        if (notaPortaria != null && !notaPortaria.isBlank()) {
            this.notaPortaria = notaPortaria;
        }
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
    public boolean isEscaladoSindico() { return escaladoSindico; }
    public String getFotoAnexo() { return fotoAnexo; }
    public String getNotaPortaria() { return notaPortaria; }
}

package com.condoeconomy.api.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class Condominio {
    private final UUID id;
    private final String nome;
    private final String cnpj;
    private final String tenantId;
    private final LocalDateTime dataCriacao;
    private boolean ativo;

    public Condominio(UUID id, String nome, String cnpj, String tenantId) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do condomínio é obrigatório");
        }
        if (cnpj == null || cnpj.isBlank()) {
            throw new IllegalArgumentException("O CNPJ é obrigatório");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("O Tenant ID é obrigatório");
        }
        
        this.id = id;
        this.nome = nome;
        this.cnpj = cnpj;
        this.tenantId = tenantId.toLowerCase().replaceAll("[^a-z0-9]", "");
        this.dataCriacao = LocalDateTime.now();
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void ativar() {
        this.ativo = true;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getCnpj() { return cnpj; }
    public String getTenantId() { return tenantId; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public boolean isAtivo() { return ativo; }
}

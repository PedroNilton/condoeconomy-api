package com.condoeconomy.api.domain.entity;

import java.util.UUID;

public class Morador {
    private final UUID id;
    private final String nome;
    private final String telefone;
    private final UUID unidadeId; // Referência à Unidade

    public Morador(UUID id, String nome, String telefone, UUID unidadeId) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do morador é obrigatório");
        }
        if (unidadeId == null) {
            throw new IllegalArgumentException("O morador deve estar vinculado a uma unidade");
        }
        this.id = id;
        this.nome = nome;
        this.telefone = telefone;
        this.unidadeId = unidadeId;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getTelefone() { return telefone; }
    public UUID getUnidadeId() { return unidadeId; }
}

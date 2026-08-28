package com.condoeconomy.api.domain.entity;

import java.util.UUID;

public class Unidade {
    private final UUID id;
    private final String bloco;
    private final String numero;

    public Unidade(UUID id, String bloco, String numero) {
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("O número da unidade é obrigatório");
        }
        this.id = id;
        this.bloco = bloco;
        this.numero = numero;
    }

    public UUID getId() { return id; }
    public String getBloco() { return bloco; }
    public String getNumero() { return numero; }
}

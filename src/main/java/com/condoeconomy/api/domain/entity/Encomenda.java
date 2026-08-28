package com.condoeconomy.api.domain.entity;

import com.condoeconomy.api.domain.enums.StatusEncomenda;
import java.time.LocalDateTime;
import java.util.UUID;

public class Encomenda {
    private final UUID id;
    private final String codigoRastreio;
    private final String transportadora;
    private final UUID unidadeId; // Para quem é a encomenda
    private StatusEncomenda status;
    private final LocalDateTime dataRecebimento;
    private LocalDateTime dataRetirada;

    public Encomenda(UUID id, String codigoRastreio, String transportadora, UUID unidadeId) {
        if (unidadeId == null) {
            throw new IllegalArgumentException("A unidade de destino é obrigatória");
        }
        this.id = id;
        this.codigoRastreio = codigoRastreio;
        this.transportadora = transportadora;
        this.unidadeId = unidadeId;
        this.status = StatusEncomenda.AGUARDANDO_RETIRADA;
        this.dataRecebimento = LocalDateTime.now();
    }

    public void registrarRetirada() {
        if (this.status != StatusEncomenda.AGUARDANDO_RETIRADA) {
            throw new IllegalStateException("Só é possível retirar encomendas que estão aguardando");
        }
        this.status = StatusEncomenda.RETIRADA;
        this.dataRetirada = LocalDateTime.now();
    }

    // Getters
    public UUID getId() { return id; }
    public String getCodigoRastreio() { return codigoRastreio; }
    public String getTransportadora() { return transportadora; }
    public UUID getUnidadeId() { return unidadeId; }
    public StatusEncomenda getStatus() { return status; }
    public LocalDateTime getDataRecebimento() { return dataRecebimento; }
    public LocalDateTime getDataRetirada() { return dataRetirada; }
}

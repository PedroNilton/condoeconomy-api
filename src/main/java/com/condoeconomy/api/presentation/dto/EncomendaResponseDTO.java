package com.condoeconomy.api.presentation.dto;

import com.condoeconomy.api.domain.entity.Encomenda;
import java.time.LocalDateTime;
import java.util.UUID;

public record EncomendaResponseDTO(
        UUID id, 
        String codigoRastreio, 
        String transportadora, 
        UUID unidadeId, 
        String status, 
        LocalDateTime dataRecebimento
) {
    public static EncomendaResponseDTO fromEntity(Encomenda e) {
        return new EncomendaResponseDTO(
                e.getId(),
                e.getCodigoRastreio(),
                e.getTransportadora(),
                e.getUnidadeId(),
                e.getStatus().name(),
                e.getDataRecebimento()
        );
    }
}

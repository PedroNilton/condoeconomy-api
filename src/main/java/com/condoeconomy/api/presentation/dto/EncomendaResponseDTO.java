package com.condoeconomy.api.presentation.dto;

import com.condoeconomy.api.domain.entity.Encomenda;
import java.time.LocalDateTime;
import java.util.UUID;

public record EncomendaResponseDTO(
        UUID id, 
        String codigoRastreio, 
        String transportadora, 
        String destinatario,
        String unidade,
        String status, 
        LocalDateTime dataChegada,
        LocalDateTime dataRetirada
) {
    public static EncomendaResponseDTO fromEntity(Encomenda e) {
        return new EncomendaResponseDTO(
                e.getId(),
                e.getCodigoRastreio(),
                e.getTransportadora(),
                e.getDestinatario(),
                e.getUnidade(),
                e.getStatus().name(),
                e.getDataRecebimento(),
                e.getDataRetirada()
        );
    }
}

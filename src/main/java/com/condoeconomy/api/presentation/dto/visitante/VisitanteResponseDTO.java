package com.condoeconomy.api.presentation.dto.visitante;

import com.condoeconomy.api.domain.entity.visitante.Visitante;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record VisitanteResponseDTO(
        UUID id,
        String nome,
        String sobrenome,
        String documento,
        LocalDate dataVisita,
        String blocoDestino,
        String unidadeDestino,
        String moradorResponsavel,
        String placaVeiculo,
        String tipo,
        String status,
        LocalDateTime horaEntrada,
        LocalDateTime horaSaida
) {
    public static VisitanteResponseDTO fromEntity(Visitante entity) {
        return new VisitanteResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getSobrenome(),
                entity.getDocumento(),
                entity.getDataVisita(),
                entity.getBlocoDestino(),
                entity.getUnidadeDestino(),
                entity.getMoradorResponsavel(),
                entity.getPlacaVeiculo(),
                entity.getTipo(),
                entity.getStatus(),
                entity.getHoraEntrada(),
                entity.getHoraSaida()
        );
    }
}

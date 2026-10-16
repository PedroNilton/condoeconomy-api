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
                mascararDocumento(entity.getDocumento()),
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

    public static String mascararDocumento(String doc) {
        if (doc == null || doc.isBlank()) {
            return null;
        }
        String limpo = doc.replaceAll("[^0-9a-zA-Z]", "");
        if (limpo.length() <= 4) {
            return "****";
        }
        if (limpo.length() == 11) {
            // Mascaramento no padrão LGPD para CPF: ***.123.456-**
            return "***." + limpo.substring(3, 6) + "." + limpo.substring(6, 9) + "-**";
        }
        int len = limpo.length();
        return "***." + limpo.substring(len - 4, len - 2) + "**";
    }
}

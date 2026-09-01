package com.condoeconomy.api.presentation.dto.reserva;

import com.condoeconomy.api.domain.entity.reserva.Convidado;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConvidadoResponseDTO(
    UUID id,
    String nome,
    String documento,
    String statusEntrada,
    LocalDateTime horaEntrada
) {
    public static ConvidadoResponseDTO fromEntity(Convidado c) {
        return new ConvidadoResponseDTO(c.getId(), c.getNome(), c.getDocumento(), c.getStatusEntrada().name(), c.getHoraEntrada());
    }
}

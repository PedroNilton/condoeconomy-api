package com.condoeconomy.api.presentation.dto.reserva;

import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.domain.entity.reserva.Convidado;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConvidadoResponseDTO(
    UUID id,
    String nome,
    String documento,
    String statusEntrada,
    LocalDateTime horaEntrada,
    boolean adicionadoPortaria
) {
    public static ConvidadoResponseDTO fromEntity(Convidado c) {
        boolean extra = "PORTARIA".equalsIgnoreCase(c.getDocumento());
        return new ConvidadoResponseDTO(
            c.getId(),
            c.getNome(),
            extra ? "PORTARIA" : SecuritySanitizer.mascararDocumento(c.getDocumento()),
            c.getStatusEntrada().name(),
            c.getHoraEntrada(),
            extra
        );
    }
}

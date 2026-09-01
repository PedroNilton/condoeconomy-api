package com.condoeconomy.api.presentation.dto.reserva;

import com.condoeconomy.api.domain.entity.reserva.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record ReservaResponseDTO(
    UUID id,
    String nomeArea,
    String unidade,
    String moradorSolicitante,
    String titulo,
    LocalDate dataReserva,
    LocalTime horaInicio,
    LocalTime horaFim,
    String status,
    List<ConvidadoResponseDTO> convidados
) {
    public static ReservaResponseDTO fromEntity(Reserva r) {
        return new ReservaResponseDTO(
            r.getId(),
            r.getAreaComum().getNome(),
            r.getUnidadeTexto(),
            r.getMoradorSolicitante(),
            r.getTitulo(),
            r.getDataReserva(),
            r.getHoraInicio(),
            r.getHoraFim(),
            r.getStatus().name(),
            r.getConvidados().stream().map(ConvidadoResponseDTO::fromEntity).collect(Collectors.toList())
        );
    }
}

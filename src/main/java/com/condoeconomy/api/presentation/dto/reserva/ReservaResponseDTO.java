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
    String areaComumNome,
    String unidade,
    String unidadeTexto,
    String moradorSolicitante,
    String titulo,
    LocalDate dataReserva,
    LocalTime horaInicio,
    LocalTime horaFim,
    String status,
    List<ConvidadoResponseDTO> convidados,
    String motivoRejeicao
) {
    public static ReservaResponseDTO fromEntity(Reserva r) {
        String area = r.getAreaComum() != null ? r.getAreaComum().getNome() : "Área Comum";
        String un = r.getUnidadeTexto();
        return new ReservaResponseDTO(
            r.getId(),
            area,
            area,
            un,
            un,
            r.getMoradorSolicitante(),
            r.getTitulo(),
            r.getDataReserva(),
            r.getHoraInicio(),
            r.getHoraFim(),
            r.getStatus().name(),
            r.getConvidados() != null
                ? r.getConvidados().stream().map(ConvidadoResponseDTO::fromEntity).collect(Collectors.toList())
                : List.of(),
            r.getMotivoRejeicao()
        );
    }
}

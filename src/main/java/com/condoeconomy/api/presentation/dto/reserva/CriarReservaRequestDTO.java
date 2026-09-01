package com.condoeconomy.api.presentation.dto.reserva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CriarReservaRequestDTO(
    @NotNull UUID areaComumId,
    @NotBlank String unidade,
    @NotBlank String morador,
    @NotBlank String titulo,
    @NotNull LocalDate data,
    @NotNull LocalTime inicio,
    @NotNull LocalTime fim,
    List<ConvidadoInput> convidados
) {
    public record ConvidadoInput(
        @NotBlank String nome,
        String documento
    ) {}
}

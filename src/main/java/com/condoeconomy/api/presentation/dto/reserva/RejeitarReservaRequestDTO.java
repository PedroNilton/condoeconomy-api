package com.condoeconomy.api.presentation.dto.reserva;

import jakarta.validation.constraints.Size;

public record RejeitarReservaRequestDTO(
        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres")
        String motivo
) {}

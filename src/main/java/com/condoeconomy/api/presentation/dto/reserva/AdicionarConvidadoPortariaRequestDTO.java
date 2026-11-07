package com.condoeconomy.api.presentation.dto.reserva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdicionarConvidadoPortariaRequestDTO(
        @NotBlank(message = "O nome do convidado é obrigatório")
        @Size(max = 120, message = "O nome do convidado deve ter no máximo 120 caracteres")
        String nome
) {}

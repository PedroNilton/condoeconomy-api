package com.condoeconomy.api.presentation.dto.chamado;

import jakarta.validation.constraints.Size;

public record EscalarChamadoRequestDTO(
    @Size(max = 1000, message = "A nota da portaria deve ter no máximo 1000 caracteres")
    String notaPortaria
) {}

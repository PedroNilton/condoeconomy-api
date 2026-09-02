package com.condoeconomy.api.presentation.dto.chamado;

import jakarta.validation.constraints.NotBlank;

public record AtualizarStatusChamadoRequestDTO(
    @NotBlank String novoStatus
) {}

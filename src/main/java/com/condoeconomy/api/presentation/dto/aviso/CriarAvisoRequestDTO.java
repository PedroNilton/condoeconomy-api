package com.condoeconomy.api.presentation.dto.aviso;

import jakarta.validation.constraints.NotBlank;

public record CriarAvisoRequestDTO(
    @NotBlank String titulo,
    @NotBlank String mensagem,
    @NotBlank String autor
) {}

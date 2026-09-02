package com.condoeconomy.api.presentation.dto.chamado;

import jakarta.validation.constraints.NotBlank;

public record CriarChamadoRequestDTO(
    @NotBlank String unidadeTexto,
    @NotBlank String moradorSolicitante,
    @NotBlank String categoria,
    @NotBlank String assunto,
    @NotBlank String descricao
) {}

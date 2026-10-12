package com.condoeconomy.api.presentation.dto.chamado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarChamadoRequestDTO(
    String unidadeTexto,
    String moradorSolicitante,
    @NotBlank(message = "A categoria é obrigatória") @Size(max = 50, message = "Categoria deve ter no máximo 50 caracteres") String categoria,
    @NotBlank(message = "O assunto é obrigatório") @Size(min = 3, max = 150, message = "O assunto deve ter entre 3 e 150 caracteres") String assunto,
    @NotBlank(message = "A descrição é obrigatória") @Size(min = 5, max = 2000, message = "A descrição deve ter entre 5 e 2000 caracteres") String descricao
) {}

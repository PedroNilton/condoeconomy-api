package com.condoeconomy.api.presentation.dto.aviso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarAvisoRequestDTO(
    @NotBlank(message = "O título é obrigatório") @Size(min = 3, max = 150, message = "O título deve ter entre 3 e 150 caracteres") String titulo,
    @NotBlank(message = "A mensagem é obrigatória") @Size(min = 5, max = 3000, message = "A mensagem deve ter entre 5 e 3000 caracteres") String mensagem,
    @Size(max = 100, message = "O nome do autor deve ter no máximo 100 caracteres") String autor
) {}

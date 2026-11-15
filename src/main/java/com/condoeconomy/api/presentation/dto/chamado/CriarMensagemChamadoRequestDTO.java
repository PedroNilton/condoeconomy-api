package com.condoeconomy.api.presentation.dto.chamado;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarMensagemChamadoRequestDTO(
    @NotBlank(message = "A mensagem é obrigatória")
    @Size(min = 1, max = 2000, message = "A mensagem deve ter no máximo 2000 caracteres")
    String texto
) {}

package com.condoeconomy.api.presentation.dto.visitante;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarVisitanteRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
        String nome,

        @NotBlank(message = "O sobrenome é obrigatório")
        @Size(min = 2, max = 100, message = "O sobrenome deve ter entre 2 e 100 caracteres")
        String sobrenome,

        @NotBlank(message = "O bloco de destino é obrigatório")
        @Size(max = 20, message = "O bloco deve ter no máximo 20 caracteres")
        String blocoDestino,

        @NotBlank(message = "A unidade de destino é obrigatória")
        @Size(max = 20, message = "A unidade deve ter no máximo 20 caracteres")
        String unidadeDestino,

        @Size(max = 10, message = "A placa deve ter no máximo 10 caracteres")
        String placaVeiculo
) {
}

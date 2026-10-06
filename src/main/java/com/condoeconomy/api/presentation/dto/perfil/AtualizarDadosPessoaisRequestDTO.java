package com.condoeconomy.api.presentation.dto.perfil;

import jakarta.validation.constraints.Size;

public record AtualizarDadosPessoaisRequestDTO(
        @Size(max = 255) String nome,
        @Size(max = 20) String telefone,
        @Size(max = 10) String apartamento,
        @Size(max = 50) String bloco,
        String foto
) {}

package com.condoeconomy.api.presentation.dto.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdicionarMoradorRequestDTO(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Size(max = 30) String parentesco
) {}

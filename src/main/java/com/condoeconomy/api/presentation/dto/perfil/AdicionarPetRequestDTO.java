package com.condoeconomy.api.presentation.dto.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdicionarPetRequestDTO(
        @NotBlank @Size(max = 50) String nome,
        @NotBlank @Size(max = 30) String especie,
        @NotBlank @Size(max = 50) String raca
) {}

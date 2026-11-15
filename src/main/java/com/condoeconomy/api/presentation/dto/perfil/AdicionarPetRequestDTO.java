package com.condoeconomy.api.presentation.dto.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdicionarPetRequestDTO(
        @NotBlank @Size(max = 50) String nome,
        @NotBlank @Size(max = 30) String especie,
        @Size(max = 50) String raca,
        @Size(max = 40) String cor,
        String fotoUrl
) {
    public AdicionarPetRequestDTO(String nome, String especie, String raca) {
        this(nome, especie, raca, null, null);
    }
}

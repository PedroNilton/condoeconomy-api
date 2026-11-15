package com.condoeconomy.api.presentation.dto.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdicionarVeiculoRequestDTO(
        @NotBlank @Size(max = 10) String placa,
        @NotBlank @Size(max = 50) String modelo,
        @NotBlank @Size(max = 30) String cor,
        @Size(max = 30) String vaga
) {
    public AdicionarVeiculoRequestDTO(String placa, String modelo, String cor) {
        this(placa, modelo, cor, null);
    }
}

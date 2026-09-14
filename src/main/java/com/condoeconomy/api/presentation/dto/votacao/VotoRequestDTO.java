package com.condoeconomy.api.presentation.dto.votacao;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class VotoRequestDTO {
    @NotNull(message = "A opção escolhida é obrigatória")
    private UUID opcaoId;
}

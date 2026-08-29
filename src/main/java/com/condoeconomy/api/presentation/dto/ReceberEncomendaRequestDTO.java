package com.condoeconomy.api.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReceberEncomendaRequestDTO(
        @NotBlank(message = "O código de rastreio é obrigatório") String codigoRastreio,
        @NotBlank(message = "O nome da transportadora é obrigatório") String transportadora,
        @NotNull(message = "O ID da unidade de destino é obrigatório") UUID unidadeId
) {}

package com.condoeconomy.api.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CNPJ;

public record CondominioRequestDTO(
        @NotBlank(message = "O nome é obrigatório") String nome,
        @NotBlank(message = "O CNPJ é obrigatório") @CNPJ(message = "CNPJ inválido") String cnpj,
        @NotBlank(message = "O identificador do tenant (tenantId) é obrigatório") String tenantId
) {}

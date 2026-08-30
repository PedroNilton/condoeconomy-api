package com.condoeconomy.api.presentation.dto;

import com.condoeconomy.api.domain.entity.Condominio;
import java.time.LocalDateTime;
import java.util.UUID;

public record CondominioResponseDTO(
        UUID id,
        String nome,
        String cnpj,
        String tenantId,
        LocalDateTime dataCriacao,
        boolean ativo
) {
    public static CondominioResponseDTO fromEntity(Condominio c) {
        return new CondominioResponseDTO(
                c.getId(),
                c.getNome(),
                c.getCnpj(),
                c.getTenantId(),
                c.getDataCriacao(),
                c.isAtivo()
        );
    }
}

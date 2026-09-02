package com.condoeconomy.api.presentation.dto.chamado;

import com.condoeconomy.api.domain.entity.chamado.Chamado;
import java.time.LocalDateTime;
import java.util.UUID;

public record ChamadoResponseDTO(
    UUID id,
    String unidadeTexto,
    String moradorSolicitante,
    String categoria,
    String assunto,
    String descricao,
    String status,
    LocalDateTime dataAbertura,
    LocalDateTime dataResolucao
) {
    public static ChamadoResponseDTO fromEntity(Chamado c) {
        return new ChamadoResponseDTO(
            c.getId(), c.getUnidadeTexto(), c.getMoradorSolicitante(), 
            c.getCategoria().name(), c.getAssunto(), c.getDescricao(), 
            c.getStatus().name(), c.getDataAbertura(), c.getDataResolucao()
        );
    }
}

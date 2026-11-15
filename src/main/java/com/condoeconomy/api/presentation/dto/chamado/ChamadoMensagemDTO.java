package com.condoeconomy.api.presentation.dto.chamado;

import com.condoeconomy.api.infrastructure.persistence.entity.chamado.ChamadoMensagemJpaEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChamadoMensagemDTO(
    UUID id,
    UUID chamadoId,
    String autorNome,
    String autorPapel,
    String texto,
    LocalDateTime enviadaEm
) {
    public static ChamadoMensagemDTO fromEntity(ChamadoMensagemJpaEntity e) {
        return new ChamadoMensagemDTO(
            e.getId(),
            e.getChamadoId(),
            e.getAutorNome(),
            e.getAutorPapel(),
            e.getTexto(),
            e.getEnviadaEm()
        );
    }
}

package com.condoeconomy.api.presentation.dto.reserva;

import com.condoeconomy.api.domain.entity.reserva.AreaComum;
import java.math.BigDecimal;
import java.util.UUID;

public record AreaComumResponseDTO(
    UUID id,
    String nome,
    String descricao,
    Integer capacidadeMaxima,
    BigDecimal taxaLocacao
) {
    public static AreaComumResponseDTO fromEntity(AreaComum a) {
        return new AreaComumResponseDTO(a.getId(), a.getNome(), a.getDescricao(), a.getCapacidadeMaxima(), a.getTaxaLocacao());
    }
}

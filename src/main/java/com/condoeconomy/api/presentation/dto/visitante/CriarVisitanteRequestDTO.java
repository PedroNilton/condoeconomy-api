package com.condoeconomy.api.presentation.dto.visitante;

public record CriarVisitanteRequestDTO(
        String nome,
        String sobrenome,
        String blocoDestino,
        String unidadeDestino,
        String placaVeiculo
) {
}

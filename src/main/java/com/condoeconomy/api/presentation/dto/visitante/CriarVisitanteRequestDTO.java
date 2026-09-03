package com.condoeconomy.api.presentation.dto.visitante;

import java.time.LocalDate;

public record CriarVisitanteRequestDTO(
        String nome,
        String documento,
        LocalDate dataVisita,
        String unidadeDestino,
        String moradorResponsavel,
        String tipo
) {}

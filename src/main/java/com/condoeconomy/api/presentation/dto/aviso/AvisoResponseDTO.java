package com.condoeconomy.api.presentation.dto.aviso;

import java.time.LocalDateTime;

public record AvisoResponseDTO(
    String id,
    String titulo,
    String mensagem,
    LocalDateTime dataCriacao,
    String autor
) {}

package com.condoeconomy.api.presentation.dto.votacao;

import lombok.Data;

import java.util.UUID;

@Data
public class OpcaoVotacaoDTO {
    private UUID id;
    private String titulo;
}

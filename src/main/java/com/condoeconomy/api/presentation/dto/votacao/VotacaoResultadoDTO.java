package com.condoeconomy.api.presentation.dto.votacao;

import com.condoeconomy.api.domain.enums.VotacaoStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class VotacaoResultadoDTO {
    private UUID id;
    private String titulo;
    private VotacaoStatus status;
    private int totalVotos;
    private List<OpcaoResultadoDTO> opcoes;
}

package com.condoeconomy.api.presentation.dto.votacao;

import com.condoeconomy.api.domain.enums.VotacaoStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class VotacaoResponseDTO {
    private UUID id;
    private String titulo;
    private String descricao;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataEncerramento;
    private VotacaoStatus status;
    private String autorNome;
    private List<OpcaoVotacaoDTO> opcoes;
    private boolean usuarioJaVotou;
}

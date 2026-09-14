package com.condoeconomy.api.presentation.dto.votacao;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class VotacaoRequestDTO {
    @NotBlank(message = "O título é obrigatório")
    private String titulo;
    
    private String descricao;
    
    @NotNull(message = "A data de encerramento é obrigatória")
    @Future(message = "A data de encerramento deve estar no futuro")
    private LocalDateTime dataEncerramento;
    
    @NotEmpty(message = "Deve haver pelo menos uma opção")
    private List<String> opcoes;
}

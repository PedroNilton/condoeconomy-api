package com.condoeconomy.api.presentation.dto.votacao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpcaoResultadoDTO {
    private UUID opcaoId;
    private String titulo;
    private int votos;
    private double percentual;
}

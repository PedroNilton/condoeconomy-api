package com.condoeconomy.api.presentation.controller.portaria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VeiculoConsultaDTO {
    private UUID id;
    private String placa;
    private String modelo;
    private String cor;
    
    // Dados do morador
    private UUID moradorId;
    private String nomeMorador;
    private String apartamento;
    private String bloco;
}

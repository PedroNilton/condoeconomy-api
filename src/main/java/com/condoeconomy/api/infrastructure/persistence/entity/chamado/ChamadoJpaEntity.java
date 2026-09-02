package com.condoeconomy.api.infrastructure.persistence.entity.chamado;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chamado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChamadoJpaEntity {
    @Id
    private UUID id;
    private String unidadeTexto;
    private String moradorSolicitante;
    private String categoria;
    private String assunto;
    private String descricao;
    private String status;
    private LocalDateTime dataAbertura;
    private LocalDateTime dataResolucao;
}

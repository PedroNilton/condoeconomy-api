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
    private boolean escaladoSindico;
    private String fotoAnexo;
    @jakarta.persistence.Column(name = "nota_portaria")
    private String notaPortaria;
    @jakarta.persistence.Column(name = "usuario_id")
    private UUID usuarioId;

    public ChamadoJpaEntity(UUID id, String unidadeTexto, String moradorSolicitante, String categoria,
                            String assunto, String descricao, String status, LocalDateTime dataAbertura,
                            LocalDateTime dataResolucao, boolean escaladoSindico, String fotoAnexo, String notaPortaria) {
        this(id, unidadeTexto, moradorSolicitante, categoria, assunto, descricao, status, dataAbertura,
                dataResolucao, escaladoSindico, fotoAnexo, notaPortaria, null);
    }
}

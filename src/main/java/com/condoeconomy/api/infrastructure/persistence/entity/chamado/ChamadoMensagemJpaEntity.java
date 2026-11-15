package com.condoeconomy.api.infrastructure.persistence.entity.chamado;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chamado_mensagem")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChamadoMensagemJpaEntity {
    @Id
    private UUID id;

    @Column(name = "chamado_id", nullable = false)
    private UUID chamadoId;

    @Column(name = "autor_nome")
    private String autorNome;

    @Column(name = "autor_papel")
    private String autorPapel;

    @Column(nullable = false)
    private String texto;

    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm;
}

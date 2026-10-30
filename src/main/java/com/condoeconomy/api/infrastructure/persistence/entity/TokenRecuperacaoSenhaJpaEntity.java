package com.condoeconomy.api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "token_recuperacao_senha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRecuperacaoSenhaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(name = "usado", nullable = false)
    private boolean usado = false;

    @Column(name = "ip_solicitante", length = 64)
    private String ipSolicitante;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}

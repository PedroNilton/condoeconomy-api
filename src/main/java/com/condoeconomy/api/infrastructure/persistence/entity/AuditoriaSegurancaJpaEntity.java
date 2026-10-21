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
@Table(name = "auditoria_seguranca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaSegurancaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "usuario_email", nullable = false)
    private String usuarioEmail;

    @Column(name = "papel")
    private String papel;

    @Column(name = "evento", nullable = false, length = 80)
    private String evento;

    @Column(name = "severidade", nullable = false, length = 20)
    private String severidade; // INFO, ALERTA, CRITICO

    @Column(name = "ip_origem", length = 64)
    private String ipOrigem;

    @Column(name = "detalhes", length = 500)
    private String detalhes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}

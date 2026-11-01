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
@Table(name = "preferencia_notificacao_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaNotificacaoJpaEntity {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "encomendas_push", nullable = false)
    private boolean encomendasPush = true;

    @Column(name = "boletos_push", nullable = false)
    private boolean boletosPush = true;

    @Column(name = "boletos_email", nullable = false)
    private boolean boletosEmail = true;

    @Column(name = "reservas_push", nullable = false)
    private boolean reservasPush = true;

    @Column(name = "comunicados_push", nullable = false)
    private boolean comunicadosPush = true;

    @Column(name = "comunicados_email", nullable = false)
    private boolean comunicadosEmail = true;

    @Column(name = "resumo_email", nullable = false)
    private boolean resumoEmail = true;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public static PreferenciaNotificacaoJpaEntity padrao(UUID usuarioId) {
        return new PreferenciaNotificacaoJpaEntity(
                usuarioId, true, true, true, true, true, true, true, LocalDateTime.now()
        );
    }
}

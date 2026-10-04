package com.condoeconomy.api.infrastructure.persistence.entity.reserva;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.envers.Audited;
import com.condoeconomy.api.domain.entity.reserva.Reserva;

@Entity
@Table(name = "reserva")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class ReservaJpaEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_comum_id")
    @org.hibernate.envers.Audited(targetAuditMode = org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED)
    private AreaComumJpaEntity areaComum;

    private String unidadeTexto;
    private String moradorSolicitante;
    private String titulo;
    private LocalDate dataReserva;
    private LocalTime horaInicio;
    private LocalTime horaFim;
    private String status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataSolicitacao;

    @Column(name = "motivo_rejeicao")
    private String motivoRejeicao;

    @OneToMany(mappedBy = "reserva", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.envers.NotAudited
    private List<ConvidadoJpaEntity> convidados;

}

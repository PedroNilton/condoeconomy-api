package com.condoeconomy.api.infrastructure.persistence.entity.reserva;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "convidado_reserva")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvidadoJpaEntity {
    @Id
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private ReservaJpaEntity reserva;
    
    private String nome;
    private String documento;
    private String statusEntrada;
    private LocalDateTime horaEntrada;
}

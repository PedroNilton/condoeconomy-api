package com.condoeconomy.api.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "morador_adicional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoradorAdicionalJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    private String nome;
    private String parentesco;
}

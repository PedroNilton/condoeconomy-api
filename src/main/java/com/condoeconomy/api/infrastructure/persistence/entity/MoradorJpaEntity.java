package com.condoeconomy.api.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "morador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoradorJpaEntity {
    
    @Id
    private UUID id;
    
    private String nome;
    private String telefone;
    
    @Column(name = "unidade_id")
    private UUID unidadeId;
}

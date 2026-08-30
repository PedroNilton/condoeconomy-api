package com.condoeconomy.api.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "condominio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CondominioJpaEntity {

    @Id
    private UUID id;

    private String nome;
    
    @Column(unique = true)
    private String cnpj;
    
    @Column(name = "tenant_id", unique = true)
    private String tenantId;
    
    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;
    
    private boolean ativo;
}

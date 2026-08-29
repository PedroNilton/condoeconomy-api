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
@Table(name = "encomenda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EncomendaJpaEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "codigo_rastreio")
    private String codigoRastreio;
    
    private String transportadora;
    private String status;
    
    @Column(name = "data_recebimento")
    private LocalDateTime dataRecebimento;
    
    @Column(name = "data_retirada")
    private LocalDateTime dataRetirada;
    
    @Column(name = "unidade_id")
    private UUID unidadeId;
}

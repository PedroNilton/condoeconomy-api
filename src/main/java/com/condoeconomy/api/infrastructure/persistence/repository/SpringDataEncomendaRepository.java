package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.infrastructure.persistence.entity.EncomendaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SpringDataEncomendaRepository extends JpaRepository<EncomendaJpaEntity, UUID> {
    
    @Query("SELECT e FROM EncomendaJpaEntity e WHERE LOWER(:nome) LIKE LOWER(CONCAT('%', e.destinatario, '%')) OR LOWER(e.destinatario) LIKE LOWER(CONCAT('%', :nome, '%')) ORDER BY e.dataRecebimento DESC")
    List<EncomendaJpaEntity> findFlexibleByDestinatario(@Param("nome") String nome);
}

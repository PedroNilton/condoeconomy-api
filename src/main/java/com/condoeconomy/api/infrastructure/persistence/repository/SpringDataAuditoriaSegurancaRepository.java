package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.infrastructure.persistence.entity.AuditoriaSegurancaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAuditoriaSegurancaRepository extends JpaRepository<AuditoriaSegurancaJpaEntity, UUID> {
    List<AuditoriaSegurancaJpaEntity> findTop15ByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);
    List<AuditoriaSegurancaJpaEntity> findTop50ByOrderByCriadoEmDesc();
}

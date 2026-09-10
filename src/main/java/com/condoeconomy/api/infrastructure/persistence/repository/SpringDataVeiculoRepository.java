package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.infrastructure.persistence.entity.VeiculoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

import java.util.Optional;

public interface SpringDataVeiculoRepository extends JpaRepository<VeiculoJpaEntity, UUID> {
    List<VeiculoJpaEntity> findByUsuarioId(UUID usuarioId);
    Optional<VeiculoJpaEntity> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}

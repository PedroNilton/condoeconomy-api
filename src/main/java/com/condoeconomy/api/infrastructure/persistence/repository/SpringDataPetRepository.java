package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.infrastructure.persistence.entity.PetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

import java.util.Optional;

public interface SpringDataPetRepository extends JpaRepository<PetJpaEntity, UUID> {
    List<PetJpaEntity> findByUsuarioId(UUID usuarioId);
    Optional<PetJpaEntity> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}

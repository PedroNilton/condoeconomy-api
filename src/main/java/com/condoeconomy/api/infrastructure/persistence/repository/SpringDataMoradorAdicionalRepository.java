package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.infrastructure.persistence.entity.MoradorAdicionalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataMoradorAdicionalRepository extends JpaRepository<MoradorAdicionalJpaEntity, UUID> {
    List<MoradorAdicionalJpaEntity> findByUsuarioId(UUID usuarioId);
}

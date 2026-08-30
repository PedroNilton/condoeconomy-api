package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.infrastructure.persistence.entity.CondominioJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCondominioRepository extends JpaRepository<CondominioJpaEntity, UUID> {
    Optional<CondominioJpaEntity> findByCnpj(String cnpj);
    boolean existsByTenantId(String tenantId);
}

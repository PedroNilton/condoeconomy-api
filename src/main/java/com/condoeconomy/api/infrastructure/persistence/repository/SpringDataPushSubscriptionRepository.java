package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataPushSubscriptionRepository extends JpaRepository<PushSubscriptionJpaEntity, UUID> {
    List<PushSubscriptionJpaEntity> findByUsuarioId(UUID usuarioId);
    void deleteByEndpoint(String endpoint);
}

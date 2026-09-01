package com.condoeconomy.api.infrastructure.persistence.repository.reserva;

import com.condoeconomy.api.infrastructure.persistence.entity.reserva.AreaComumJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SpringDataAreaComumRepository extends JpaRepository<AreaComumJpaEntity, UUID> {
}

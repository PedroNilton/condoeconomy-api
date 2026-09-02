package com.condoeconomy.api.infrastructure.persistence.repository.chamado;

import com.condoeconomy.api.infrastructure.persistence.entity.chamado.ChamadoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataChamadoRepository extends JpaRepository<ChamadoJpaEntity, UUID> {
    List<ChamadoJpaEntity> findAllByOrderByDataAberturaDesc();
}

package com.condoeconomy.api.infrastructure.persistence.repository.chamado;

import com.condoeconomy.api.infrastructure.persistence.entity.chamado.ChamadoMensagemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataChamadoMensagemRepository extends JpaRepository<ChamadoMensagemJpaEntity, UUID> {
    List<ChamadoMensagemJpaEntity> findByChamadoIdOrderByEnviadaEmAsc(UUID chamadoId);
}

package com.condoeconomy.api.infrastructure.persistence.repository.financeiro;

import com.condoeconomy.api.infrastructure.persistence.entity.financeiro.BoletoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataBoletoRepository extends JpaRepository<BoletoJpaEntity, UUID> {
    List<BoletoJpaEntity> findByUnidadeTextoContainingIgnoreCaseOrderByDataVencimentoDesc(String unidadeTexto);
    List<BoletoJpaEntity> findAllByOrderByDataVencimentoDesc();
}

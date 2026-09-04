package com.condoeconomy.api.infrastructure.persistence.repository.financeiro;

import com.condoeconomy.api.infrastructure.persistence.entity.financeiro.DespesaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.UUID;

public interface SpringDataDespesaRepository extends JpaRepository<DespesaJpaEntity, UUID> {
    @Query("SELECT SUM(d.valor) FROM DespesaJpaEntity d")
    BigDecimal sumTotalDespesas();
}

package com.condoeconomy.api.infrastructure.persistence.repository.financeiro;

import com.condoeconomy.api.infrastructure.persistence.entity.financeiro.BoletoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataBoletoRepository extends JpaRepository<BoletoJpaEntity, UUID> {
    List<BoletoJpaEntity> findByUnidadeTextoContainingIgnoreCaseOrderByDataVencimentoDesc(String unidadeTexto);
    List<BoletoJpaEntity> findAllByOrderByDataVencimentoDesc();
    List<BoletoJpaEntity> findTop5ByOrderByDataVencimentoDesc();
    
    @org.springframework.data.jpa.repository.Query("SELECT SUM(b.valor) FROM BoletoJpaEntity b")
    java.math.BigDecimal sumTotalEsperado();
    
    @org.springframework.data.jpa.repository.Query("SELECT SUM(b.valor) FROM BoletoJpaEntity b WHERE b.status = 'PAGO'")
    java.math.BigDecimal sumTotalPago();
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT b.unidadeTexto) FROM BoletoJpaEntity b WHERE b.status = 'VENCIDO'")
    Long countUnidadesInadimplentes();
    
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT b.unidadeTexto) FROM BoletoJpaEntity b")
    Long countTotalUnidades();
}

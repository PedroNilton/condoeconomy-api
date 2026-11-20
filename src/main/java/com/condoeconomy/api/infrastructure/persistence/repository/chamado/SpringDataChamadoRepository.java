package com.condoeconomy.api.infrastructure.persistence.repository.chamado;

import com.condoeconomy.api.infrastructure.persistence.entity.chamado.ChamadoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataChamadoRepository extends JpaRepository<ChamadoJpaEntity, UUID> {
    List<ChamadoJpaEntity> findAllByOrderByDataAberturaDesc();
    List<ChamadoJpaEntity> findByMoradorSolicitanteOrderByDataAberturaDesc(String moradorSolicitante);

    @org.springframework.data.jpa.repository.Query(
            "SELECT c FROM ChamadoJpaEntity c WHERE c.usuarioId = :usuarioId OR (c.usuarioId IS NULL AND LOWER(c.moradorSolicitante) = LOWER(:moradorSolicitante)) ORDER BY c.dataAbertura DESC"
    )
    List<ChamadoJpaEntity> findByUsuarioIdOrLegacyMorador(
            @org.springframework.data.repository.query.Param("usuarioId") UUID usuarioId,
            @org.springframework.data.repository.query.Param("moradorSolicitante") String moradorSolicitante
    );
}

package com.condoeconomy.api.infrastructure.repository;

import com.condoeconomy.api.domain.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VotoRepository extends JpaRepository<Voto, UUID> {
    boolean existsByVotacaoIdAndUsuarioId(UUID votacaoId, UUID usuarioId);
    List<Voto> findByVotacaoId(UUID votacaoId);
}

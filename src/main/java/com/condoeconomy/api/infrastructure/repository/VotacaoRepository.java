package com.condoeconomy.api.infrastructure.repository;

import com.condoeconomy.api.domain.entity.Votacao;
import com.condoeconomy.api.domain.enums.VotacaoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VotacaoRepository extends JpaRepository<Votacao, UUID> {
    List<Votacao> findByStatusOrderByDataAberturaDesc(VotacaoStatus status);
}

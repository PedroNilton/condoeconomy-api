package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.infrastructure.persistence.entity.PreferenciaNotificacaoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataPreferenciaNotificacaoRepository extends JpaRepository<PreferenciaNotificacaoJpaEntity, UUID> {
}

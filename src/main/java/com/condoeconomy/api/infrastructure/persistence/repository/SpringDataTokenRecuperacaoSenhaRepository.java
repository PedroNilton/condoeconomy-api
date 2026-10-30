package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.infrastructure.persistence.entity.TokenRecuperacaoSenhaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTokenRecuperacaoSenhaRepository extends JpaRepository<TokenRecuperacaoSenhaJpaEntity, UUID> {

    Optional<TokenRecuperacaoSenhaJpaEntity> findFirstByUsuarioIdAndTokenHashAndUsadoFalseOrderByCriadoEmDesc(
            UUID usuarioId,
            String tokenHash
    );

    @Modifying
    @Query("UPDATE TokenRecuperacaoSenhaJpaEntity t SET t.usado = true WHERE t.usuarioId = :usuarioId AND t.usado = false")
    void invalidarTokensAnteriores(@Param("usuarioId") UUID usuarioId);
}

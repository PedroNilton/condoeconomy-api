package com.condoeconomy.api.infrastructure.repository.notificacao;

import com.condoeconomy.api.domain.entity.notificacao.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {
    List<Notificacao> findByUsuarioIdOrderByDataCriacaoDesc(UUID usuarioId);
    long countByUsuarioIdAndLidaFalse(UUID usuarioId);

    @Modifying
    @Transactional
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.usuarioId = :usuarioId AND n.lida = false")
    int marcarTodasComoLidasPorUsuario(@Param("usuarioId") UUID usuarioId);
}

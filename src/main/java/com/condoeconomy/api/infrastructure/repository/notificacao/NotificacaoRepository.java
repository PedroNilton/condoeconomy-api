package com.condoeconomy.api.infrastructure.repository.notificacao;

import com.condoeconomy.api.domain.entity.notificacao.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {
    List<Notificacao> findByUsuarioIdOrderByDataCriacaoDesc(UUID usuarioId);
    long countByUsuarioIdAndLidaFalse(UUID usuarioId);
}

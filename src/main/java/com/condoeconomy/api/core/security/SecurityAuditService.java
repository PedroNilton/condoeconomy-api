package com.condoeconomy.api.core.security;

import com.condoeconomy.api.infrastructure.persistence.entity.AuditoriaSegurancaJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataAuditoriaSegurancaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço central de Trilha de Auditoria Forense (Defense-in-Depth & Conformidade LGPD).
 * Persiste eventos críticos de segurança e controle de acesso no banco de dados.
 */
@Service
public class SecurityAuditService {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditService.class);

    private final SpringDataAuditoriaSegurancaRepository repository;

    public SecurityAuditService(SpringDataAuditoriaSegurancaRepository repository) {
        this.repository = repository;
    }

    public void registrar(UUID usuarioId, String email, String papel, String evento, String severidade, String ip, String detalhes) {
        try {
            AuditoriaSegurancaJpaEntity registro = new AuditoriaSegurancaJpaEntity(
                    UUID.randomUUID(),
                    usuarioId,
                    email != null && !email.isBlank() ? SecuritySanitizer.sanitizar(email) : "DESCONHECIDO",
                    papel != null ? papel : "N/A",
                    evento,
                    severidade != null ? severidade : "INFO",
                    ip != null ? ip : "N/A",
                    detalhes != null && detalhes.length() > 500 ? detalhes.substring(0, 500) : detalhes,
                    LocalDateTime.now()
            );
            repository.save(registro);
        } catch (Exception e) {
            log.error("[FALHA DE AUDITORIA] Não foi possível persistir evento forense '{}': {}", evento, e.getMessage());
        }
    }

    public void registrarUsuario(UsuarioJpaEntity usuario, String evento, String severidade, String ip, String detalhes) {
        if (usuario == null) {
            registrar(null, "ANONIMO", "N/A", evento, severidade, ip, detalhes);
            return;
        }
        registrar(usuario.getId(), usuario.getEmail(), usuario.getPapel(), evento, severidade, ip, detalhes);
    }

    public List<AuditoriaSegurancaJpaEntity> listarPorUsuario(UUID usuarioId) {
        return repository.findTop15ByUsuarioIdOrderByCriadoEmDesc(usuarioId);
    }

    public List<AuditoriaSegurancaJpaEntity> listarRecentesCondominio() {
        return repository.findTop50ByOrderByCriadoEmDesc();
    }
}

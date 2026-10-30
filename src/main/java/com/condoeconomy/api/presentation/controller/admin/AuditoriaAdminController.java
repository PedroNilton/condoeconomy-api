package com.condoeconomy.api.presentation.controller.admin;

import com.condoeconomy.api.core.security.SecurityAuditService;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.infrastructure.persistence.entity.AuditoriaSegurancaJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/auditoria")
@PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
public class AuditoriaAdminController {

    private final SecurityAuditService auditService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listarEventosForenses(
            @RequestParam(required = false) String severidade) {
        String filtroSeveridade = null;
        if (severidade != null && !severidade.isBlank()) {
            SecuritySanitizer.validarTextoSeguro(severidade, "severidade");
            filtroSeveridade = SecuritySanitizer.sanitizar(severidade).toUpperCase();
        }

        List<AuditoriaSegurancaJpaEntity> eventos = auditService.listarParaPainelAdmin(filtroSeveridade);
        Map<String, Long> resumo = auditService.obterResumoSeveridade();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("resumo", resumo);
        resposta.put("eventos", eventos);
        return ResponseEntity.ok(resposta);
    }
}

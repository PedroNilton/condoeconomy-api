package com.condoeconomy.api.presentation.controller.financeiro;

import com.condoeconomy.api.application.usecase.financeiro.DarBaixaBoletoUseCase;
import com.condoeconomy.api.application.usecase.financeiro.ListarBoletosUseCase;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.presentation.dto.financeiro.BoletoResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/boletos")
public class BoletoController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BoletoController.class);

    private final ListarBoletosUseCase listarBoletosUseCase;
    private final DarBaixaBoletoUseCase darBaixaBoletoUseCase;

    public BoletoController(ListarBoletosUseCase listarBoletosUseCase, DarBaixaBoletoUseCase darBaixaBoletoUseCase) {
        this.listarBoletosUseCase = listarBoletosUseCase;
        this.darBaixaBoletoUseCase = darBaixaBoletoUseCase;
    }

    @GetMapping
    public ResponseEntity<List<BoletoResponseDTO>> listarBoletos(
            Authentication auth,
            @RequestParam(required = false) String unidade) {
        if (unidade != null && !unidade.isBlank()) {
            SecuritySanitizer.validarTextoSeguro(unidade, "unidade");
        }

        com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity user = 
            (com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity) auth.getPrincipal();
            
        String unidadeConsulta = unidade != null ? SecuritySanitizer.sanitizar(unidade) : null;
        
        // Se for morador, obriga a buscar SOMENTE da própria unidade (proteção anti-IDOR)
        if ("ROLE_MORADOR".equals(user.getPapel())) {
            String unidadeMorador = "Apto " + user.getApartamento() + " - Bloco " + user.getBloco();
            if (unidadeConsulta != null && !unidadeConsulta.equalsIgnoreCase(unidadeMorador)) {
                log.warn("[AUDITORIA DE SEGURANÇA] Tentativa de consulta de boletos de outra unidade ('{}') pelo morador '{}' ({})",
                        unidadeConsulta, user.getEmail(), unidadeMorador);
            }
            unidadeConsulta = unidadeMorador;
        }

        var boletos = listarBoletosUseCase.executar(unidadeConsulta);
        var dtos = boletos.stream().map(BoletoResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/baixar")
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<BoletoResponseDTO> darBaixa(@PathVariable UUID id, Authentication auth) {
        log.info("[AUDITORIA FINANCEIRA] Baixa manual registrada no boleto ID={} pelo operador '{}'", id, auth != null ? auth.getName() : "ANONYMOUS");
        var boleto = darBaixaBoletoUseCase.executar(id);
        return ResponseEntity.ok(BoletoResponseDTO.fromEntity(boleto));
    }
}

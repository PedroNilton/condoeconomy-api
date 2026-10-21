package com.condoeconomy.api.presentation.controller.financeiro;

import com.condoeconomy.api.application.usecase.financeiro.DarBaixaBoletoUseCase;
import com.condoeconomy.api.application.usecase.financeiro.ListarBoletosUseCase;
import com.condoeconomy.api.core.security.SecurityAuditService;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.presentation.dto.financeiro.BoletoResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/boletos")
public class BoletoController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BoletoController.class);

    private final ListarBoletosUseCase listarBoletosUseCase;
    private final DarBaixaBoletoUseCase darBaixaBoletoUseCase;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService auditService;

    public BoletoController(ListarBoletosUseCase listarBoletosUseCase,
                            DarBaixaBoletoUseCase darBaixaBoletoUseCase,
                            PasswordEncoder passwordEncoder,
                            SecurityAuditService auditService) {
        this.listarBoletosUseCase = listarBoletosUseCase;
        this.darBaixaBoletoUseCase = darBaixaBoletoUseCase;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<BoletoResponseDTO>> listarBoletos(
            Authentication auth,
            @RequestParam(required = false) String unidade,
            HttpServletRequest request) {
        if (unidade != null && !unidade.isBlank()) {
            SecuritySanitizer.validarTextoSeguro(unidade, "unidade");
        }

        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();

        String unidadeConsulta = unidade != null ? SecuritySanitizer.sanitizar(unidade) : null;

        // Se for morador, obriga a buscar SOMENTE da própria unidade (proteção anti-IDOR)
        if ("ROLE_MORADOR".equals(user.getPapel())) {
            String unidadeMorador = "Apto " + user.getApartamento() + " - Bloco " + user.getBloco();
            if (unidadeConsulta != null && !unidadeConsulta.equalsIgnoreCase(unidadeMorador)) {
                log.warn("[AUDITORIA DE SEGURANÇA] Tentativa de consulta de boletos de outra unidade ('{}') pelo morador '{}' ({})",
                        unidadeConsulta, user.getEmail(), unidadeMorador);
                auditService.registrarUsuario(user, "TENTATIVA_IDOR_BOLETO", "ALERTA", request.getRemoteAddr(),
                        "Tentativa bloqueada de consultar boletos da unidade: " + unidadeConsulta);
            }
            unidadeConsulta = unidadeMorador;
        }

        var boletos = listarBoletosUseCase.executar(unidadeConsulta);
        var dtos = boletos.stream().map(BoletoResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/baixar")
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> darBaixa(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Step-Up-Password", required = false) String stepUpHeader,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth,
            HttpServletRequest request) {
        UsuarioJpaEntity operador = (UsuarioJpaEntity) auth.getPrincipal();
        String ip = request.getRemoteAddr();

        String senhaConfirmacao = stepUpHeader;
        if ((senhaConfirmacao == null || senhaConfirmacao.isBlank()) && body != null) {
            senhaConfirmacao = body.get("senhaConfirmacao");
        }

        // Barreira Step-Up Auth: exige confirmação de senha para operação financeira crítica (baixa manual)
        if (senhaConfirmacao == null || senhaConfirmacao.isBlank() || !passwordEncoder.matches(senhaConfirmacao, operador.getSenha())) {
            log.warn("[AUDITORIA FINANCEIRA] Tentativa de baixa manual sem Step-Up Auth válido no boleto ID={} pelo operador '{}'",
                    id, operador.getEmail());
            auditService.registrarUsuario(operador, "FALHA_STEP_UP_BAIXA_BOLETO", "ALERTA", ip,
                    "Tentativa de baixa manual no boleto ID=" + id + " rejeitada por falta de confirmação de senha.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Confirmação de segurança obrigatória: informe sua senha atual válida para realizar a baixa manual do boleto."));
        }

        var boleto = darBaixaBoletoUseCase.executar(id);
        log.info("[AUDITORIA FINANCEIRA] Baixa manual registrada no boleto ID={} pelo operador '{}' com Step-Up Auth", id, operador.getEmail());
        auditService.registrarUsuario(operador, "BAIXA_MANUAL_BOLETO", "INFO", ip,
                "Baixa manual confirmada via Step-Up Auth para o boleto ID=" + id + " (" + boleto.getUnidadeTexto() + ").");
        return ResponseEntity.ok(BoletoResponseDTO.fromEntity(boleto));
    }
}

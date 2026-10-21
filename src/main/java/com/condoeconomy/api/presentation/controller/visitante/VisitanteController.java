package com.condoeconomy.api.presentation.controller.visitante;

import com.condoeconomy.api.application.usecase.visitante.AlterarStatusVisitanteUseCase;
import com.condoeconomy.api.application.usecase.visitante.CriarVisitanteUseCase;
import com.condoeconomy.api.application.usecase.visitante.ListarVisitantesUseCase;
import com.condoeconomy.api.core.security.LoginAttemptService;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.presentation.dto.visitante.CriarVisitanteRequestDTO;
import com.condoeconomy.api.presentation.dto.visitante.VisitanteResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/visitantes")
public class VisitanteController {

    private static final Logger log = LoggerFactory.getLogger(VisitanteController.class);

    private final CriarVisitanteUseCase criarVisitanteUseCase;
    private final ListarVisitantesUseCase listarVisitantesUseCase;
    private final AlterarStatusVisitanteUseCase alterarStatusVisitanteUseCase;
    private final LoginAttemptService loginAttemptService;

    public VisitanteController(CriarVisitanteUseCase criarVisitanteUseCase,
                               ListarVisitantesUseCase listarVisitantesUseCase,
                               AlterarStatusVisitanteUseCase alterarStatusVisitanteUseCase,
                               LoginAttemptService loginAttemptService) {
        this.criarVisitanteUseCase = criarVisitanteUseCase;
        this.listarVisitantesUseCase = listarVisitantesUseCase;
        this.alterarStatusVisitanteUseCase = alterarStatusVisitanteUseCase;
        this.loginAttemptService = loginAttemptService;
    }

    @GetMapping
    public ResponseEntity<List<VisitanteResponseDTO>> listar(
            Authentication auth,
            @RequestParam(required = false) String unidadeDestino,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataVisita) {
        if (unidadeDestino != null && !unidadeDestino.isBlank()) {
            SecuritySanitizer.validarTextoSeguro(unidadeDestino, "unidadeDestino");
        }

        String unidadeConsulta = unidadeDestino != null ? SecuritySanitizer.sanitizar(unidadeDestino) : null;

        // Proteção LGPD / Anti-IDOR: Morador só pode consultar visitantes da própria unidade
        if (auth != null && auth.getPrincipal() instanceof UsuarioJpaEntity user && "ROLE_MORADOR".equals(user.getPapel())) {
            String aptoMorador = user.getApartamento();
            String unidadeFormatada = "Apto " + user.getApartamento() + " - Bloco " + user.getBloco();
            if (unidadeConsulta == null || (!unidadeConsulta.equalsIgnoreCase(aptoMorador) && !unidadeConsulta.equalsIgnoreCase(unidadeFormatada))) {
                unidadeConsulta = aptoMorador;
            }
            var visitantes = listarVisitantesUseCase.executar(unidadeConsulta, dataVisita);
            if (visitantes.isEmpty() && !unidadeConsulta.equalsIgnoreCase(unidadeFormatada)) {
                visitantes = listarVisitantesUseCase.executar(unidadeFormatada, dataVisita);
            }
            var dtos = visitantes.stream().map(VisitanteResponseDTO::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        }

        var visitantes = listarVisitantesUseCase.executar(unidadeConsulta, dataVisita);
        var dtos = visitantes.stream().map(VisitanteResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> criar(
            @RequestBody @Valid CriarVisitanteRequestDTO request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        // Proteção Anti-Flood (Rate Limiting por IP) para o fluxo público de Auto Check-in na portaria
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioJpaEntity)) {
            String ip = httpRequest.getRemoteAddr();
            if (!loginAttemptService.verificarEContabilizarRequisicaoPublica(ip, "auto-checkin-visitante", 10)) {
                log.warn("[AUDITORIA DE SEGURANÇA] Rate Limit excedido no Auto Check-in de visitantes para o IP {}", ip);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("message", "Muitas solicitações de check-in em sequência. Aguarde alguns minutos."));
            }
        }

        SecuritySanitizer.validarTextoSeguro(request.nome(), "nome");
        SecuritySanitizer.validarTextoSeguro(request.sobrenome(), "sobrenome");
        SecuritySanitizer.validarTextoSeguro(request.blocoDestino(), "blocoDestino");
        SecuritySanitizer.validarTextoSeguro(request.unidadeDestino(), "unidadeDestino");
        if (request.placaVeiculo() != null && !request.placaVeiculo().isBlank()) {
            SecuritySanitizer.validarTextoSeguro(request.placaVeiculo(), "placaVeiculo");
        }

        String bloco = SecuritySanitizer.sanitizar(request.blocoDestino());
        String unidade = SecuritySanitizer.sanitizar(request.unidadeDestino());

        // Se for morador autenticado, vincula obrigatoriamente ao seu bloco/apartamento
        if (auth != null && auth.getPrincipal() instanceof UsuarioJpaEntity user && "ROLE_MORADOR".equals(user.getPapel())) {
            if (user.getBloco() != null && !user.getBloco().isBlank()) {
                bloco = user.getBloco();
            }
            if (user.getApartamento() != null && !user.getApartamento().isBlank()) {
                unidade = user.getApartamento();
            }
        }

        var visitante = criarVisitanteUseCase.executar(
                SecuritySanitizer.sanitizar(request.nome()),
                SecuritySanitizer.sanitizar(request.sobrenome()),
                bloco,
                unidade,
                request.placaVeiculo() != null ? SecuritySanitizer.sanitizar(request.placaVeiculo()) : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(VisitanteResponseDTO.fromEntity(visitante));
    }

    @PutMapping("/{id}/checkin")
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<VisitanteResponseDTO> checkin(@PathVariable UUID id, Authentication auth) {
        var visitante = alterarStatusVisitanteUseCase.registrarEntrada(id);
        log.info("[AUDITORIA DE PORTARIA] Check-in do visitante ID={} ('{} {}') registrado pelo operador '{}'",
                id, visitante.getNome(), visitante.getSobrenome(), auth != null ? auth.getName() : "SISTEMA");
        return ResponseEntity.ok(VisitanteResponseDTO.fromEntity(visitante));
    }

    @PutMapping("/{id}/checkout")
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<VisitanteResponseDTO> checkout(@PathVariable UUID id, Authentication auth) {
        var visitante = alterarStatusVisitanteUseCase.registrarSaida(id);
        log.info("[AUDITORIA DE PORTARIA] Check-out do visitante ID={} ('{} {}') registrado pelo operador '{}'",
                id, visitante.getNome(), visitante.getSobrenome(), auth != null ? auth.getName() : "SISTEMA");
        return ResponseEntity.ok(VisitanteResponseDTO.fromEntity(visitante));
    }
}

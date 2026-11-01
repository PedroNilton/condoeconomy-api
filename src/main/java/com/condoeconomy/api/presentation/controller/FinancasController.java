package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.service.payment.GatewayPagamentoService;
import com.condoeconomy.api.core.security.SecurityAuditService;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.infrastructure.persistence.entity.AuditoriaSegurancaJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.financeiro.BoletoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataBoletoRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataDespesaRepository;
import com.condoeconomy.api.presentation.dto.financeiro.DashboardFinanceiroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/financas")
@RequiredArgsConstructor
public class FinancasController {

    private final SpringDataBoletoRepository boletoRepository;
    private final SpringDataDespesaRepository despesaRepository;
    private final GatewayPagamentoService gatewayPagamentoService;
    private final SecurityAuditService auditService;

    public record EmitirBoletoRequestDTO(
            @NotBlank(message = "A identificação da unidade é obrigatória")
            @Size(max = 100, message = "Unidade deve ter no máximo 100 caracteres")
            String unidadeTexto,

            @NotBlank(message = "O nome do morador responsável é obrigatório")
            @Size(max = 120, message = "Morador responsável deve ter no máximo 120 caracteres")
            String moradorResponsavel,

            @NotBlank(message = "A competência é obrigatória")
            @Pattern(regexp = "^\\d{2}/\\d{4}$", message = "A competência deve estar no formato MM/AAAA")
            String competencia,

            @NotNull(message = "O valor do boleto é obrigatório")
            @DecimalMin(value = "1.00", message = "O valor mínimo para emissão é R$ 1,00")
            BigDecimal valor,

            @NotNull(message = "A data de vencimento é obrigatória")
            LocalDate dataVencimento
    ) {}

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<DashboardFinanceiroResponse> getDashboard() {

        BigDecimal totalEsperado = boletoRepository.sumTotalEsperado();
        BigDecimal totalPago = boletoRepository.sumTotalPago();
        BigDecimal totalDespesas = despesaRepository.sumTotalDespesas();

        Long inadimplentes = boletoRepository.countUnidadesInadimplentes();
        Long totalUnidades = boletoRepository.countTotalUnidades();

        if (totalEsperado == null) totalEsperado = BigDecimal.ZERO;
        if (totalPago == null) totalPago = BigDecimal.ZERO;
        if (totalDespesas == null) totalDespesas = BigDecimal.ZERO;
        if (inadimplentes == null) inadimplentes = 0L;
        if (totalUnidades == null || totalUnidades == 0) totalUnidades = 1L;

        double inadimplenciaPct = ((double) inadimplentes / totalUnidades) * 100.0;
        inadimplenciaPct = Math.round(inadimplenciaPct * 10.0) / 10.0;

        var boletos = boletoRepository.findTop5ByOrderByDataVencimentoDesc().stream().map(b ->
            DashboardFinanceiroResponse.BoletoResumo.builder()
                .id(b.getId().toString())
                .unidade(b.getUnidadeTexto())
                .valor(b.getValor())
                .status(b.getStatus())
                .data(b.getDataVencimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .build()
        ).collect(Collectors.toList());

        DashboardFinanceiroResponse response = DashboardFinanceiroResponse.builder()
                .receitaPrevista(totalEsperado)
                .receitaArrecadada(totalPago)
                .despesas(totalDespesas)
                .unidadesInadimplentes(inadimplentes)
                .inadimplencia(inadimplenciaPct)
                .boletosRecentes(boletos)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping({"/boletos", "/boletos-gerados"})
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<DashboardFinanceiroResponse.BoletoResumo>> getBoletos() {
        var boletos = boletoRepository.findAllByOrderByDataVencimentoDesc().stream().map(b ->
            DashboardFinanceiroResponse.BoletoResumo.builder()
                .id(b.getId().toString())
                .unidade(b.getUnidadeTexto())
                .valor(b.getValor())
                .status(b.getStatus())
                .data(b.getDataVencimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .build()
        ).collect(Collectors.toList());
        return ResponseEntity.ok(boletos);
    }

    @PostMapping("/boletos")
    @Transactional
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> emitirBoleto(
            @RequestBody @Valid EmitirBoletoRequestDTO dto,
            @AuthenticationPrincipal UsuarioJpaEntity operador,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();
        try {
            SecuritySanitizer.validarTextoSeguro(dto.unidadeTexto(), "unidadeTexto");
            SecuritySanitizer.validarTextoSeguro(dto.moradorResponsavel(), "moradorResponsavel");
            SecuritySanitizer.validarTextoSeguro(dto.competencia(), "competencia");
        } catch (IllegalArgumentException e) {
            auditService.registrarUsuario(operador, "TENTATIVA_INJECAO_EMISSAO_BOLETO", "CRITICO", ip, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }

        UUID id = UUID.randomUUID();
        String unidadeLimpa = SecuritySanitizer.sanitizar(dto.unidadeTexto());
        String moradorLimpo = SecuritySanitizer.sanitizar(dto.moradorResponsavel());
        String competenciaLimpa = SecuritySanitizer.sanitizar(dto.competencia());

        String linhaDigitavel = gatewayPagamentoService.gerarBoletoReal(id, dto.valor(), "00000000000", moradorLimpo);
        String statusInicial = dto.dataVencimento().isBefore(LocalDate.now()) ? "VENCIDO" : "PENDENTE";

        BoletoJpaEntity novo = new BoletoJpaEntity(
                id,
                unidadeLimpa,
                moradorLimpo,
                competenciaLimpa,
                dto.valor(),
                dto.dataVencimento(),
                null,
                statusInicial,
                linhaDigitavel,
                null
        );
        BoletoJpaEntity salvo = boletoRepository.save(novo);

        auditService.registrarUsuario(operador, "BOLETO_EMITIDO_ADMIN", "INFO", ip,
                "Boleto emitido para '" + salvo.getUnidadeTexto() + "' (Competência " + salvo.getCompetencia() + ", Valor R$ " + salvo.getValor() + ").");

        return ResponseEntity.status(HttpStatus.CREATED).body(
                DashboardFinanceiroResponse.BoletoResumo.builder()
                        .id(salvo.getId().toString())
                        .unidade(salvo.getUnidadeTexto())
                        .valor(salvo.getValor())
                        .status(salvo.getStatus())
                        .data(salvo.getDataVencimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .build()
        );
    }

    @GetMapping("/auditoria-seguranca")
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<AuditoriaSegurancaJpaEntity>> listarAuditoriaCondominio() {
        return ResponseEntity.ok(auditService.listarRecentesCondominio());
    }
}

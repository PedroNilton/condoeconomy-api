package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.usecase.CadastrarCondominioUseCase;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.presentation.dto.CondominioRequestDTO;
import com.condoeconomy.api.presentation.dto.CondominioResponseDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/condominios")
public class CondominioController {

    private static final Logger log = LoggerFactory.getLogger(CondominioController.class);

    private final CadastrarCondominioUseCase useCase;

    public CondominioController(CadastrarCondominioUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    public ResponseEntity<CondominioResponseDTO> cadastrar(
            @Valid @RequestBody CondominioRequestDTO request,
            Authentication auth) {
        SecuritySanitizer.validarTextoSeguro(request.nome(), "nome");
        SecuritySanitizer.validarTextoSeguro(request.cnpj(), "cnpj");
        SecuritySanitizer.validarTextoSeguro(request.tenantId(), "tenantId");

        if (request.tenantId() == null || !request.tenantId().matches("^[a-zA-Z0-9_]{1,63}$")) {
            throw new IllegalArgumentException("O identificador de tenant informado é inválido.");
        }

        var condominio = useCase.executar(
                SecuritySanitizer.sanitizar(request.nome()),
                SecuritySanitizer.sanitizar(request.cnpj()),
                request.tenantId().trim()
        );
        String operador = auth != null ? auth.getName() : "SUPER_ADMIN";
        log.info("[AUDITORIA DE PLATAFORMA] Novo condomínio '{}' (tenant='{}') provisionado pelo operador '{}'",
                condominio.getNome(), condominio.getTenantId(), operador);
        return ResponseEntity.status(HttpStatus.CREATED).body(CondominioResponseDTO.fromEntity(condominio));
    }
}

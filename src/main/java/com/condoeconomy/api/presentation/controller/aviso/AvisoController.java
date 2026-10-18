package com.condoeconomy.api.presentation.controller.aviso;

import com.condoeconomy.api.application.usecase.aviso.CriarAvisoUseCase;
import com.condoeconomy.api.application.usecase.aviso.ListarAvisosUseCase;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.presentation.dto.aviso.CriarAvisoRequestDTO;
import com.condoeconomy.api.presentation.dto.aviso.AvisoResponseDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/avisos")
public class AvisoController {

    private static final Logger log = LoggerFactory.getLogger(AvisoController.class);

    private final CriarAvisoUseCase criarAvisoUseCase;
    private final ListarAvisosUseCase listarAvisosUseCase;

    public AvisoController(CriarAvisoUseCase criarAvisoUseCase, ListarAvisosUseCase listarAvisosUseCase) {
        this.criarAvisoUseCase = criarAvisoUseCase;
        this.listarAvisosUseCase = listarAvisosUseCase;
    }

    @GetMapping
    public ResponseEntity<List<AvisoResponseDTO>> listar() {
        return ResponseEntity.ok(listarAvisosUseCase.execute());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<AvisoResponseDTO> criar(@RequestBody @Valid CriarAvisoRequestDTO data, Authentication auth) {
        SecuritySanitizer.validarTextoSeguro(data.titulo(), "titulo");
        SecuritySanitizer.validarTextoSeguro(data.mensagem(), "mensagem");

        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        String autorFinal = user.getNome() != null ? user.getNome() : "Síndico";

        CriarAvisoRequestDTO saneData = new CriarAvisoRequestDTO(
                SecuritySanitizer.sanitizar(data.titulo()),
                SecuritySanitizer.sanitizar(data.mensagem()),
                SecuritySanitizer.sanitizar(autorFinal)
        );
        AvisoResponseDTO response = criarAvisoUseCase.execute(saneData);
        log.info("[AUDITORIA DE COMUNICAÇÃO] Aviso '{}' publicado pelo usuário '{}'", saneData.titulo(), user.getEmail());
        return ResponseEntity.ok(response);
    }
}

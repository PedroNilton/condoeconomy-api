package com.condoeconomy.api.presentation.controller.aviso;

import com.condoeconomy.api.application.usecase.aviso.CriarAvisoUseCase;
import com.condoeconomy.api.application.usecase.aviso.ListarAvisosUseCase;
import com.condoeconomy.api.application.usecase.aviso.ExcluirAvisoUseCase;
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
    private final ExcluirAvisoUseCase excluirAvisoUseCase;
    private final com.condoeconomy.api.infrastructure.repository.aviso.AvisoRepository avisoRepository;

    public AvisoController(
            CriarAvisoUseCase criarAvisoUseCase,
            ListarAvisosUseCase listarAvisosUseCase,
            ExcluirAvisoUseCase excluirAvisoUseCase,
            com.condoeconomy.api.infrastructure.repository.aviso.AvisoRepository avisoRepository) {
        this.criarAvisoUseCase = criarAvisoUseCase;
        this.listarAvisosUseCase = listarAvisosUseCase;
        this.excluirAvisoUseCase = excluirAvisoUseCase;
        this.avisoRepository = avisoRepository;
    }

    @GetMapping
    public ResponseEntity<List<AvisoResponseDTO>> listar() {
        return ResponseEntity.ok(listarAvisosUseCase.execute());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable String id) {
        excluirAvisoUseCase.execute(id);
        log.info("[AUDITORIA DE COMUNICAÇÃO] Aviso '{}' excluído", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<AvisoResponseDTO> criar(@RequestBody @Valid CriarAvisoRequestDTO data, Authentication auth) {
        SecuritySanitizer.validarTextoSeguro(data.titulo(), "titulo");
        SecuritySanitizer.validarTextoSeguro(data.mensagem(), "mensagem");
        if (data.categoria() != null && !data.categoria().isBlank()) {
            SecuritySanitizer.validarTextoSeguro(data.categoria(), "categoria");
        }
        if (data.blocos() != null && !data.blocos().isBlank()) {
            SecuritySanitizer.validarTextoSeguro(data.blocos(), "blocos");
        }

        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        String autorFinal = user.getNome() != null ? user.getNome() : "Síndico";

        CriarAvisoRequestDTO saneData = new CriarAvisoRequestDTO(
                SecuritySanitizer.sanitizar(data.titulo()),
                SecuritySanitizer.sanitizar(data.mensagem()),
                SecuritySanitizer.sanitizar(autorFinal),
                data.categoria() != null && !data.categoria().isBlank() ? SecuritySanitizer.sanitizar(data.categoria()) : "Aviso geral",
                data.blocos() != null && !data.blocos().isBlank() ? SecuritySanitizer.sanitizar(data.blocos()) : null,
                Boolean.TRUE.equals(data.fixado())
        );
        AvisoResponseDTO response = criarAvisoUseCase.execute(saneData);
        log.info("[AUDITORIA DE COMUNICAÇÃO] Aviso '{}' publicado pelo usuário '{}'", saneData.titulo(), user.getEmail());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/fixar")
    @PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<AvisoResponseDTO> alternarFixado(@PathVariable String id) {
        return avisoRepository.findById(id)
                .map(aviso -> {
                    aviso.alternarFixado();
                    avisoRepository.save(aviso);
                    return ResponseEntity.ok(new AvisoResponseDTO(
                            aviso.getId(),
                            aviso.getTitulo(),
                            aviso.getMensagem(),
                            aviso.getDataCriacao(),
                            aviso.getAutor(),
                            aviso.getCategoria(),
                            aviso.getBlocos(),
                            aviso.isFixado(),
                            aviso.getVisualizacoes()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/visualizar")
    public ResponseEntity<AvisoResponseDTO> registrarVisualizacao(@PathVariable String id) {
        return avisoRepository.findById(id)
                .map(aviso -> {
                    aviso.registrarVisualizacao();
                    avisoRepository.save(aviso);
                    return ResponseEntity.ok(new AvisoResponseDTO(
                            aviso.getId(),
                            aviso.getTitulo(),
                            aviso.getMensagem(),
                            aviso.getDataCriacao(),
                            aviso.getAutor(),
                            aviso.getCategoria(),
                            aviso.getBlocos(),
                            aviso.isFixado(),
                            aviso.getVisualizacoes()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

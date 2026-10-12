package com.condoeconomy.api.presentation.controller.chamado;

import com.condoeconomy.api.application.usecase.chamado.AtualizarStatusChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.EscalarChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.CriarChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.ListarChamadosUseCase;
import com.condoeconomy.api.domain.entity.chamado.Chamado;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.presentation.dto.chamado.AtualizarStatusChamadoRequestDTO;
import com.condoeconomy.api.presentation.dto.chamado.ChamadoResponseDTO;
import com.condoeconomy.api.presentation.dto.chamado.CriarChamadoRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chamados")
public class ChamadoController {

    private final ListarChamadosUseCase listarChamadosUseCase;
    private final AtualizarStatusChamadoUseCase atualizarStatusChamadoUseCase;
    private final CriarChamadoUseCase criarChamadoUseCase;
    private final EscalarChamadoUseCase escalarChamadoUseCase;

    public ChamadoController(ListarChamadosUseCase listarChamadosUseCase, 
                             AtualizarStatusChamadoUseCase atualizarStatusChamadoUseCase,
                             CriarChamadoUseCase criarChamadoUseCase,
                             EscalarChamadoUseCase escalarChamadoUseCase) {
        this.listarChamadosUseCase = listarChamadosUseCase;
        this.atualizarStatusChamadoUseCase = atualizarStatusChamadoUseCase;
        this.criarChamadoUseCase = criarChamadoUseCase;
        this.escalarChamadoUseCase = escalarChamadoUseCase;
    }

    /**
     * Lista apenas os chamados do morador autenticado (proteção de privacidade e LGPD).
     */
    @GetMapping("/meus")
    public ResponseEntity<List<ChamadoResponseDTO>> listarMeusChamados(Authentication auth) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        var chamados = listarChamadosUseCase.executarPorMorador(user.getNome());
        var dtos = chamados.stream().map(ChamadoResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Lista todos os chamados do condomínio.
     * Restrito à equipe de portaria, administração e síndico para evitar vazamento de dados entre vizinhos.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<ChamadoResponseDTO>> listarChamados() {
        var chamados = listarChamadosUseCase.executar();
        var dtos = chamados.stream().map(ChamadoResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Cria chamado associando obrigatoriamente a identidade do morador logado,
     * prevenindo falsidade ideológica / spoofing de unidade.
     */
    @PostMapping
    public ResponseEntity<ChamadoResponseDTO> criarChamado(
            @Valid @RequestBody CriarChamadoRequestDTO request,
            Authentication auth) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        
        String morador = user.getNome();
        String unidade = (user.getApartamento() != null && user.getBloco() != null)
                ? "Apto " + user.getApartamento() + " - Bloco " + user.getBloco()
                : (request.unidadeTexto() != null && !request.unidadeTexto().isBlank() ? request.unidadeTexto() : "Geral");

        var chamado = criarChamadoUseCase.executar(
            unidade,
            morador,
            request.categoria(),
            request.assunto(),
            request.descricao()
        );
        return ResponseEntity.ok(ChamadoResponseDTO.fromEntity(chamado));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ChamadoResponseDTO> atualizarStatus(
            @PathVariable UUID id, 
            @Valid @RequestBody AtualizarStatusChamadoRequestDTO request) {
        
        Chamado.StatusChamado status = Chamado.StatusChamado.valueOf(request.novoStatus().toUpperCase());
        var chamado = atualizarStatusChamadoUseCase.executar(id, status);
        return ResponseEntity.ok(ChamadoResponseDTO.fromEntity(chamado));
    }

    @PutMapping("/{id}/escalar")
    public ResponseEntity<Void> escalarParaSindico(@PathVariable UUID id) {
        escalarChamadoUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}

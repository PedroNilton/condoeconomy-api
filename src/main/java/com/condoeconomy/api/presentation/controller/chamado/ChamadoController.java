package com.condoeconomy.api.presentation.controller.chamado;

import com.condoeconomy.api.application.gateway.chamado.ChamadoRepository;
import com.condoeconomy.api.application.usecase.chamado.AtualizarStatusChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.EscalarChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.CriarChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.ListarChamadosUseCase;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.domain.entity.chamado.Chamado;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.chamado.ChamadoMensagemJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.chamado.SpringDataChamadoMensagemRepository;
import com.condoeconomy.api.presentation.dto.chamado.AtualizarStatusChamadoRequestDTO;
import com.condoeconomy.api.presentation.dto.chamado.ChamadoMensagemDTO;
import com.condoeconomy.api.presentation.dto.chamado.ChamadoResponseDTO;
import com.condoeconomy.api.presentation.dto.chamado.CriarChamadoRequestDTO;
import com.condoeconomy.api.presentation.dto.chamado.CriarMensagemChamadoRequestDTO;
import com.condoeconomy.api.presentation.dto.chamado.EscalarChamadoRequestDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chamados")
public class ChamadoController {

    private static final Logger log = LoggerFactory.getLogger(ChamadoController.class);

    private final ListarChamadosUseCase listarChamadosUseCase;
    private final AtualizarStatusChamadoUseCase atualizarStatusChamadoUseCase;
    private final CriarChamadoUseCase criarChamadoUseCase;
    private final EscalarChamadoUseCase escalarChamadoUseCase;
    private final ChamadoRepository chamadoRepository;
    private final SpringDataChamadoMensagemRepository mensagemRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public ChamadoController(ListarChamadosUseCase listarChamadosUseCase, 
                             AtualizarStatusChamadoUseCase atualizarStatusChamadoUseCase,
                             CriarChamadoUseCase criarChamadoUseCase,
                             EscalarChamadoUseCase escalarChamadoUseCase,
                             ChamadoRepository chamadoRepository,
                             SpringDataChamadoMensagemRepository mensagemRepository) {
        this.listarChamadosUseCase = listarChamadosUseCase;
        this.atualizarStatusChamadoUseCase = atualizarStatusChamadoUseCase;
        this.criarChamadoUseCase = criarChamadoUseCase;
        this.escalarChamadoUseCase = escalarChamadoUseCase;
        this.chamadoRepository = chamadoRepository;
        this.mensagemRepository = mensagemRepository;
    }

    public ChamadoController(ListarChamadosUseCase listarChamadosUseCase,
                             AtualizarStatusChamadoUseCase atualizarStatusChamadoUseCase,
                             CriarChamadoUseCase criarChamadoUseCase,
                             EscalarChamadoUseCase escalarChamadoUseCase) {
        this(listarChamadosUseCase, atualizarStatusChamadoUseCase, criarChamadoUseCase, escalarChamadoUseCase, null, null);
    }

    /**
     * Lista apenas os chamados do morador autenticado (proteção de privacidade e LGPD).
     */
    @GetMapping("/meus")
    public ResponseEntity<List<ChamadoResponseDTO>> listarMeusChamados(Authentication auth) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        var chamados = user.getId() != null
                ? listarChamadosUseCase.executarPorUsuario(user.getId(), user.getNome())
                : listarChamadosUseCase.executarPorMorador(user.getNome());
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
     * prevenindo falsidade ideológica / spoofing de unidade e injeção de scripts.
     */
    @PostMapping
    public ResponseEntity<ChamadoResponseDTO> criarChamado(
            @Valid @RequestBody CriarChamadoRequestDTO request,
            Authentication auth) {
        SecuritySanitizer.validarTextoSeguro(request.categoria(), "categoria");
        SecuritySanitizer.validarTextoSeguro(request.assunto(), "assunto");
        SecuritySanitizer.validarTextoSeguro(request.descricao(), "descricao");
        if (request.unidadeTexto() != null && !request.unidadeTexto().isBlank()) {
            SecuritySanitizer.validarTextoSeguro(request.unidadeTexto(), "unidadeTexto");
        }
        if (request.fotoAnexo() != null && !request.fotoAnexo().isBlank()) {
            SecuritySanitizer.validarUrlFotoSegura(request.fotoAnexo());
        }

        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        
        String morador = user.getNome();
        String unidade = (user.getApartamento() != null && user.getBloco() != null)
                ? "Apto " + user.getApartamento() + " - Bloco " + user.getBloco()
                : (request.unidadeTexto() != null && !request.unidadeTexto().isBlank()
                    ? SecuritySanitizer.sanitizar(request.unidadeTexto())
                    : "Geral");

        var chamado = criarChamadoUseCase.executar(
            unidade,
            morador,
            SecuritySanitizer.sanitizar(request.categoria()),
            SecuritySanitizer.sanitizar(request.assunto()),
            SecuritySanitizer.sanitizar(request.descricao()),
            request.fotoAnexo() != null && !request.fotoAnexo().isBlank() ? request.fotoAnexo().trim() : null
        );
        if (chamado != null && user.getId() != null && chamado.getUsuarioId() == null) {
            chamado.vincularUsuario(user.getId());
            if (chamadoRepository != null) {
                chamado = chamadoRepository.salvar(chamado);
            }
        }
        return ResponseEntity.ok(ChamadoResponseDTO.fromEntity(chamado));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ChamadoResponseDTO> atualizarStatus(
            @PathVariable UUID id, 
            @Valid @RequestBody AtualizarStatusChamadoRequestDTO request,
            Authentication auth) {
        
        Chamado.StatusChamado status = Chamado.StatusChamado.valueOf(request.novoStatus().toUpperCase());
        var chamado = atualizarStatusChamadoUseCase.executar(id, status);
        String operador = auth != null ? auth.getName() : "SISTEMA";
        log.info("[AUDITORIA DE OUVIDORIA] Chamado ID={} teve status atualizado para '{}' pelo operador '{}'",
                id, status, operador);
        return ResponseEntity.ok(ChamadoResponseDTO.fromEntity(chamado));
    }

    @PutMapping("/{id}/escalar")
    @PreAuthorize("hasRole('ROLE_PORTEIRO')")
    public ResponseEntity<Void> escalarParaSindico(
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid EscalarChamadoRequestDTO dto,
            Authentication auth) {
        String nota = null;
        if (dto != null && dto.notaPortaria() != null && !dto.notaPortaria().isBlank()) {
            SecuritySanitizer.validarTextoSeguro(dto.notaPortaria(), "notaPortaria");
            nota = SecuritySanitizer.sanitizar(dto.notaPortaria());
        }
        escalarChamadoUseCase.execute(id, nota);
        String operador = auth != null ? auth.getName() : "SISTEMA";
        log.info("[AUDITORIA DE OUVIDORIA] Chamado ID={} escalado para o Síndico pelo operador '{}'", id, operador);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/mensagens")
    public ResponseEntity<List<ChamadoMensagemDTO>> listarMensagens(@PathVariable UUID id, Authentication auth) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        var chamadoOpt = chamadoRepository.buscarPorId(id);
        if (chamadoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!podeAcessarChamado(auth, user, chamadoOpt.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        var lista = mensagemRepository.findByChamadoIdOrderByEnviadaEmAsc(id)
                .stream()
                .map(ChamadoMensagemDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/{id}/mensagens")
    public ResponseEntity<ChamadoMensagemDTO> enviarMensagem(
            @PathVariable UUID id,
            @Valid @RequestBody CriarMensagemChamadoRequestDTO dto,
            Authentication auth) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        var chamadoOpt = chamadoRepository.buscarPorId(id);
        if (chamadoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!podeAcessarChamado(auth, user, chamadoOpt.get())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        SecuritySanitizer.validarTextoSeguro(dto.texto(), "texto");
        String papel = resolverPapelMensagem(auth);
        String autorNome = user.getNome() != null ? user.getNome() : user.getEmail();

        ChamadoMensagemJpaEntity msg = new ChamadoMensagemJpaEntity(
                UUID.randomUUID(),
                id,
                SecuritySanitizer.sanitizar(autorNome),
                papel,
                SecuritySanitizer.sanitizar(dto.texto()),
                LocalDateTime.now()
        );
        var salva = mensagemRepository.save(msg);
        return ResponseEntity.status(HttpStatus.CREATED).body(ChamadoMensagemDTO.fromEntity(salva));
    }

    private boolean podeAcessarChamado(Authentication auth, UsuarioJpaEntity user, Chamado chamado) {
        boolean isStaff = auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_PORTEIRO")
                || a.getAuthority().equals("ROLE_SINDICO")
                || a.getAuthority().equals("ROLE_ADMIN")
                || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isStaff) return true;
        if (chamado.getUsuarioId() != null) {
            return user.getId() != null && chamado.getUsuarioId().equals(user.getId());
        }
        return chamado.getMoradorSolicitante() != null
                && user.getNome() != null
                && chamado.getMoradorSolicitante().equalsIgnoreCase(user.getNome());
    }

    private String resolverPapelMensagem(Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_SINDICO")
                || a.getAuthority().equals("ROLE_ADMIN")
                || a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return "SINDICO";
        }
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PORTEIRO"))) {
            return "PORTARIA";
        }
        return "MORADOR";
    }
}

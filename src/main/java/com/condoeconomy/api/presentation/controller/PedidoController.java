package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.gateway.PedidoRepository;
import com.condoeconomy.api.application.gateway.ProdutoRepository;
import com.condoeconomy.api.application.usecase.CriarPedidoUseCase;
import com.condoeconomy.api.core.security.SecurityAuditService;
import com.condoeconomy.api.domain.entity.Pedido;
import com.condoeconomy.api.domain.entity.Produto;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.presentation.dto.CriarPedidoRequestDTO;
import com.condoeconomy.api.presentation.dto.PedidoResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/condostore")
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    private final CriarPedidoUseCase criarPedidoUseCase;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final SecurityAuditService auditService;

    public PedidoController(CriarPedidoUseCase criarPedidoUseCase,
                            ProdutoRepository produtoRepository,
                            PedidoRepository pedidoRepository,
                            SecurityAuditService auditService) {
        this.criarPedidoUseCase = criarPedidoUseCase;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.auditService = auditService;
    }

    public record ProdutoResponseDTO(
            UUID id,
            String nome,
            String descricao,
            BigDecimal preco,
            int quantidadeEstoque
    ) {
        public static ProdutoResponseDTO fromDomain(Produto p) {
            return new ProdutoResponseDTO(
                    p.getId(),
                    p.getNome(),
                    p.getDescricao(),
                    p.getPreco(),
                    p.getQuantidadeEstoque()
            );
        }
    }

    @GetMapping("/produtos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProdutoResponseDTO>> listarCatalogo() {
        List<ProdutoResponseDTO> catalogo = produtoRepository.listarTodos().stream()
                .map(ProdutoResponseDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(catalogo);
    }

    @GetMapping("/pedidos/meus")
    @PreAuthorize("hasRole('ROLE_MORADOR')")
    public ResponseEntity<List<PedidoResponseDTO>> listarMeusPedidos(Authentication auth) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        return ResponseEntity.ok(pedidoRepository.listarPorMorador(user.getId()));
    }

    @PostMapping("/pedidos")
    @PreAuthorize("hasRole('ROLE_MORADOR')")
    public ResponseEntity<?> criarPedido(
            @Valid @RequestBody CriarPedidoRequestDTO request,
            Authentication auth,
            HttpServletRequest httpRequest) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        UUID moradorAutenticadoId = user.getId();
        String ip = httpRequest.getRemoteAddr();

        if (request.moradorId() != null && !request.moradorId().equals(moradorAutenticadoId)) {
            log.warn("[AUDITORIA DE SEGURANÇA] Tentativa de falsidade ideológica no CondoStore: usuário '{}' (ID={}) tentou criar pedido em nome de ID={}",
                    user.getEmail(), moradorAutenticadoId, request.moradorId());
            auditService.registrarUsuario(user, "TENTATIVA_IDOR_CONDOSTORE", "ALERTA", ip,
                    "Tentativa de enviar moradorId divergente no CondoStore (ignorado; ID autenticado imposto).");
        }

        try {
            Pedido pedido = criarPedidoUseCase.executar(moradorAutenticadoId, request.itens());
            log.info("[AUDITORIA DE CONDOSTORE] Pedido ID={} registrado para o morador '{}' (ID={})",
                    pedido.getId(), user.getEmail(), moradorAutenticadoId);
            auditService.registrarUsuario(user, "PEDIDO_CONDOSTORE_CRIADO", "INFO", ip,
                    "Compra no CondoStore concluída (Pedido ID=" + pedido.getId() + ", Total=R$ " + pedido.calcularTotal() + ").");
            return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponseDTO.fromEntity(pedido));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}

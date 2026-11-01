package com.condoeconomy.api.presentation.controller.admin;

import com.condoeconomy.api.application.gateway.PedidoRepository;
import com.condoeconomy.api.application.gateway.ProdutoRepository;
import com.condoeconomy.api.core.security.SecurityAuditService;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.domain.entity.Produto;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.presentation.dto.PedidoResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/condostore")
@PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class AdminCondoStoreController {

    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final SpringDataUsuarioRepository usuarioRepository;
    private final SecurityAuditService auditService;

    public AdminCondoStoreController(ProdutoRepository produtoRepository,
                                     PedidoRepository pedidoRepository,
                                     SpringDataUsuarioRepository usuarioRepository,
                                     SecurityAuditService auditService) {
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    public record SalvarProdutoRequestDTO(
            @NotBlank(message = "O nome do produto é obrigatório")
            @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres")
            String nome,

            @Size(max = 400, message = "A descrição deve ter no máximo 400 caracteres")
            String descricao,

            @NotNull(message = "O preço unitário é obrigatório")
            @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
            BigDecimal preco,

            @Min(value = 0, message = "O estoque não pode ser negativo")
            int quantidadeEstoque
    ) {}

    public record ProdutoAdminDTO(
            UUID id,
            String nome,
            String descricao,
            BigDecimal preco,
            int quantidadeEstoque
    ) {
        public static ProdutoAdminDTO fromDomain(Produto p) {
            return new ProdutoAdminDTO(p.getId(), p.getNome(), p.getDescricao(), p.getPreco(), p.getQuantidadeEstoque());
        }
    }

    public record PedidoAdminDTO(
            UUID id,
            String moradorNome,
            String unidade,
            String status,
            LocalDateTime dataCriacao,
            BigDecimal total,
            int quantidadeItens
    ) {}

    @GetMapping("/resumo")
    public ResponseEntity<Map<String, Object>> obterResumoCondoStore() {
        List<ProdutoAdminDTO> produtos = produtoRepository.listarTodos().stream()
                .map(ProdutoAdminDTO::fromDomain)
                .toList();

        List<PedidoResponseDTO> pedidosRaw = pedidoRepository.listarRecentes();
        Map<UUID, UsuarioJpaEntity> usuariosPorId = usuarioRepository.findAll().stream()
                .collect(Collectors.toMap(UsuarioJpaEntity::getId, u -> u, (a, b) -> a));

        List<PedidoAdminDTO> pedidos = pedidosRaw.stream().map(p -> {
            UsuarioJpaEntity u = usuariosPorId.get(p.moradorId());
            String nome = u != null ? u.getNome() : "Morador";
            String unidade = (u != null && u.getApartamento() != null)
                    ? ("Apto " + u.getApartamento() + (u.getBloco() != null ? " • Bloco " + u.getBloco() : ""))
                    : "Unidade não informada";
            int qtdItens = p.itens() == null ? 0 : p.itens().stream().mapToInt(PedidoResponseDTO.ItemPedidoResponseDTO::quantidade).sum();
            return new PedidoAdminDTO(p.id(), nome, unidade, p.status(), p.dataCriacao(), p.total(), qtdItens);
        }).toList();

        long semEstoque = produtos.stream().filter(p -> p.quantidadeEstoque() <= 0).count();
        BigDecimal receitaTotal = pedidosRaw.stream()
                .map(PedidoResponseDTO::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> metricas = new LinkedHashMap<>();
        metricas.put("totalProdutos", produtos.size());
        metricas.put("itensSemEstoque", semEstoque);
        metricas.put("totalPedidos", pedidos.size());
        metricas.put("receitaTotal", receitaTotal);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("metricas", metricas);
        resposta.put("produtos", produtos);
        resposta.put("pedidos", pedidos);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/produtos")
    @Transactional
    public ResponseEntity<?> cadastrarProduto(
            @RequestBody @Valid SalvarProdutoRequestDTO dto,
            @AuthenticationPrincipal UsuarioJpaEntity gestor,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        try {
            SecuritySanitizer.validarTextoSeguro(dto.nome(), "nome");
            SecuritySanitizer.validarTextoSeguro(dto.descricao(), "descricao");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }

        Produto novo = new Produto(
                UUID.randomUUID(),
                SecuritySanitizer.sanitizar(dto.nome()),
                dto.descricao() != null ? SecuritySanitizer.sanitizar(dto.descricao()) : "",
                dto.preco(),
                dto.quantidadeEstoque()
        );
        Produto salvo = produtoRepository.salvar(novo);

        auditService.registrarUsuario(gestor, "PRODUTO_CONDOSTORE_CRIADO", "INFO", ip,
                "Produto '" + salvo.getNome() + "' adicionado ao CondoStore (Estoque: " + salvo.getQuantidadeEstoque() + ").");

        return ResponseEntity.status(HttpStatus.CREATED).body(ProdutoAdminDTO.fromDomain(salvo));
    }

    @PutMapping("/produtos/{id}")
    @Transactional
    public ResponseEntity<?> atualizarProduto(
            @PathVariable UUID id,
            @RequestBody @Valid SalvarProdutoRequestDTO dto,
            @AuthenticationPrincipal UsuarioJpaEntity gestor,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        try {
            SecuritySanitizer.validarTextoSeguro(dto.nome(), "nome");
            SecuritySanitizer.validarTextoSeguro(dto.descricao(), "descricao");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }

        var optExistente = produtoRepository.buscarPorId(id);
        if (optExistente.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Produto não encontrado."));
        }

        Produto atualizado = new Produto(
                id,
                SecuritySanitizer.sanitizar(dto.nome()),
                dto.descricao() != null ? SecuritySanitizer.sanitizar(dto.descricao()) : "",
                dto.preco(),
                dto.quantidadeEstoque()
        );
        Produto salvo = produtoRepository.salvar(atualizado);

        auditService.registrarUsuario(gestor, "ESTOQUE_CONDOSTORE_ATUALIZADO", "INFO", ip,
                "Produto '" + salvo.getNome() + "' atualizado (Preço: R$ " + salvo.getPreco() + ", Estoque: " + salvo.getQuantidadeEstoque() + ").");

        return ResponseEntity.ok(ProdutoAdminDTO.fromDomain(salvo));
    }
}

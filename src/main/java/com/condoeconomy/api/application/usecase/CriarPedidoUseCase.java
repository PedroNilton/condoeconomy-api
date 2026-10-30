package com.condoeconomy.api.application.usecase;

import com.condoeconomy.api.application.gateway.PedidoRepository;
import com.condoeconomy.api.application.gateway.ProdutoRepository;
import com.condoeconomy.api.domain.entity.Pedido;
import com.condoeconomy.api.domain.entity.Produto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class CriarPedidoUseCase {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    public CriarPedidoUseCase(PedidoRepository pedidoRepository, ProdutoRepository produtoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Pedido executar(UUID moradorId, Map<UUID, Integer> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um produto para concluir o pedido.");
        }

        Pedido pedido = new Pedido(UUID.randomUUID(), moradorId);

        itensRequest.forEach((produtoId, quantidade) -> {
            if (quantidade == null || quantidade <= 0 || quantidade > 50) {
                throw new IllegalArgumentException("Quantidade inválida informada para o item.");
            }
            Produto produto = produtoRepository.buscarPorId(produtoId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado no catálogo: " + produtoId));

            // O domínio se encarrega de decrementar o estoque e calcular subtotal
            pedido.adicionarItem(produto, quantidade);

            // Atualiza o produto no repositório (estoque reduzido)
            produtoRepository.salvar(produto);
        });

        return pedidoRepository.salvar(pedido);
    }
}

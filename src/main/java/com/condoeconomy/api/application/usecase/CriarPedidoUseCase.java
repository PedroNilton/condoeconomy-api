package com.condoeconomy.api.application.usecase;

import com.condoeconomy.api.application.gateway.PedidoRepository;
import com.condoeconomy.api.application.gateway.ProdutoRepository;
import com.condoeconomy.api.domain.entity.Pedido;
import com.condoeconomy.api.domain.entity.Produto;
import org.springframework.stereotype.Service;

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

    public Pedido executar(UUID moradorId, Map<UUID, Integer> itensRequest) {
        Pedido pedido = new Pedido(UUID.randomUUID(), moradorId);
        
        itensRequest.forEach((produtoId, quantidade) -> {
            Produto produto = produtoRepository.buscarPorId(produtoId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + produtoId));
            
            // O domínio se encarrega de decrementar o estoque e calcular subtotal
            pedido.adicionarItem(produto, quantidade);
            
            // Atualiza o produto no repositório (estoque reduzido)
            produtoRepository.salvar(produto);
        });

        return pedidoRepository.salvar(pedido);
    }
}

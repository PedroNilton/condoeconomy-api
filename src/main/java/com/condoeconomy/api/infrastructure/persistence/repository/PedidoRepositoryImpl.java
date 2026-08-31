package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.application.gateway.PedidoRepository;
import com.condoeconomy.api.domain.entity.Pedido;
import com.condoeconomy.api.infrastructure.persistence.entity.ItemPedidoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.PedidoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
interface SpringDataPedidoRepository extends JpaRepository<PedidoJpaEntity, UUID> {}

@Component
public class PedidoRepositoryImpl implements PedidoRepository {
    private final SpringDataPedidoRepository repository;
    
    public PedidoRepositoryImpl(SpringDataPedidoRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Pedido salvar(Pedido p) {
        PedidoJpaEntity entity = new PedidoJpaEntity();
        entity.setId(p.getId());
        entity.setMoradorId(p.getMoradorId());
        entity.setStatus(p.getStatus().name());
        entity.setDataCriacao(p.getDataCriacao());
        
        List<ItemPedidoJpaEntity> itensJpa = p.getItens().stream().map(i -> 
            new ItemPedidoJpaEntity(UUID.randomUUID(), i.getProdutoId(), entity, i.getQuantidade(), i.getPrecoUnitario(), i.getSubtotal())
        ).collect(Collectors.toList());
        
        entity.setItens(itensJpa);
        repository.save(entity);
        return p;
    }
}

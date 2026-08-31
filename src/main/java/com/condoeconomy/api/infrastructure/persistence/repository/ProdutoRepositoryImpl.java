package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.application.gateway.ProdutoRepository;
import com.condoeconomy.api.domain.entity.Produto;
import com.condoeconomy.api.infrastructure.persistence.entity.ProdutoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface SpringDataProdutoRepository extends JpaRepository<ProdutoJpaEntity, UUID> {}

@Component
public class ProdutoRepositoryImpl implements ProdutoRepository {
    private final SpringDataProdutoRepository repository;
    
    public ProdutoRepositoryImpl(SpringDataProdutoRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public Optional<Produto> buscarPorId(UUID id) {
        return repository.findById(id).map(e -> new Produto(e.getId(), e.getNome(), e.getDescricao(), e.getPreco(), e.getQuantidadeEstoque()));
    }
    
    @Override
    public Produto salvar(Produto p) {
        ProdutoJpaEntity entity = new ProdutoJpaEntity(p.getId(), p.getNome(), p.getDescricao(), p.getPreco(), p.getQuantidadeEstoque());
        repository.save(entity);
        return p;
    }
}

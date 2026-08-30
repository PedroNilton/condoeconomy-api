package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.application.gateway.CondominioRepository;
import com.condoeconomy.api.domain.entity.Condominio;
import com.condoeconomy.api.infrastructure.persistence.entity.CondominioJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CondominioRepositoryImpl implements CondominioRepository {

    private final SpringDataCondominioRepository repository;

    public CondominioRepositoryImpl(SpringDataCondominioRepository repository) {
        this.repository = repository;
    }

    @Override
    public Condominio salvar(Condominio condominio) {
        CondominioJpaEntity entity = new CondominioJpaEntity(
                condominio.getId(),
                condominio.getNome(),
                condominio.getCnpj(),
                condominio.getTenantId(),
                condominio.getDataCriacao(),
                condominio.isAtivo()
        );
        repository.save(entity);
        return condominio;
    }

    @Override
    public Optional<Condominio> buscarPorCnpj(String cnpj) {
        return repository.findByCnpj(cnpj).map(this::toDomain);
    }

    @Override
    public boolean existePorTenantId(String tenantId) {
        return repository.existsByTenantId(tenantId);
    }
    
    private Condominio toDomain(CondominioJpaEntity entity) {
        Condominio condominio = new Condominio(
                entity.getId(), 
                entity.getNome(), 
                entity.getCnpj(), 
                entity.getTenantId()
        );
        if (!entity.isAtivo()) {
            condominio.desativar();
        }
        return condominio;
    }
}

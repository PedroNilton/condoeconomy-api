package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.application.gateway.EncomendaRepository;
import com.condoeconomy.api.domain.entity.Encomenda;
import com.condoeconomy.api.domain.enums.StatusEncomenda;
import com.condoeconomy.api.infrastructure.persistence.entity.EncomendaJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class EncomendaRepositoryImpl implements EncomendaRepository {

    private final SpringDataEncomendaRepository springDataRepository;

    public EncomendaRepositoryImpl(SpringDataEncomendaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Encomenda salvar(Encomenda encomenda) {
        EncomendaJpaEntity entity = new EncomendaJpaEntity();
        entity.setId(encomenda.getId());
        entity.setCodigoRastreio(encomenda.getCodigoRastreio());
        entity.setTransportadora(encomenda.getTransportadora());
        entity.setStatus(encomenda.getStatus().name());
        entity.setDataRecebimento(encomenda.getDataRecebimento());
        entity.setDataRetirada(encomenda.getDataRetirada());
        entity.setUnidadeId(encomenda.getUnidadeId());
        
        springDataRepository.save(entity);
        return encomenda;
    }

    @Override
    public Optional<Encomenda> buscarPorId(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    private Encomenda toDomain(EncomendaJpaEntity entity) {
        Encomenda encomenda = new Encomenda(
                entity.getId(), 
                entity.getCodigoRastreio(), 
                entity.getTransportadora(), 
                entity.getUnidadeId()
        );
        // Utiliza reflexão ou construtor específico na vida real para reconstruir estado
        // Aqui simulamos assumindo o fluxo básico.
        if (StatusEncomenda.RETIRADA.name().equals(entity.getStatus())) {
            encomenda.registrarRetirada();
        }
        return encomenda;
    }
}

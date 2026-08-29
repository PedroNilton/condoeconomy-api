package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.application.gateway.UnidadeRepository;
import com.condoeconomy.api.domain.entity.Unidade;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UnidadeRepositoryImpl implements UnidadeRepository {

    private final SpringDataUnidadeRepository springDataRepository;

    public UnidadeRepositoryImpl(SpringDataUnidadeRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Unidade> buscarPorId(UUID id) {
        return springDataRepository.findById(id)
                .map(jpa -> new Unidade(jpa.getId(), jpa.getBloco(), jpa.getNumero()));
    }
}

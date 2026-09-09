package com.condoeconomy.api.infrastructure.persistence.repository;

import com.condoeconomy.api.application.gateway.EncomendaRepository;
import com.condoeconomy.api.domain.entity.Encomenda;
import com.condoeconomy.api.domain.enums.StatusEncomenda;
import com.condoeconomy.api.infrastructure.persistence.entity.EncomendaJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
        entity.setDestinatario(encomenda.getDestinatario());
        entity.setUnidadeTexto(encomenda.getUnidade());
        
        springDataRepository.save(entity);
        return encomenda;
    }

    @Override
    public Optional<Encomenda> buscarPorId(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Encomenda> buscarTodas() {
        return springDataRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Encomenda> buscarPorMorador(String nome, String bloco, String apartamento) {
        String nomeBusca = nome.replaceAll("(?i)\\s*\\(Morador\\)", "").trim();
        
        String unidadeBusca = null;
        if (bloco != null && apartamento != null && !bloco.isBlank() && !apartamento.isBlank()) {
            unidadeBusca = bloco.trim() + " - " + apartamento.trim();
        }
        
        return springDataRepository.findFlexibleByDestinatarioOrUnidade(nomeBusca, unidadeBusca)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existePorCodigoRastreio(String codigoRastreio) {
        return springDataRepository.existsByCodigoRastreio(codigoRastreio);
    }

    private Encomenda toDomain(EncomendaJpaEntity entity) {
        return Encomenda.reconstituir(
                entity.getId(), 
                entity.getCodigoRastreio(), 
                entity.getTransportadora(), 
                entity.getDestinatario(),
                entity.getUnidadeTexto(),
                StatusEncomenda.valueOf(entity.getStatus()),
                entity.getDataRecebimento(),
                entity.getDataRetirada()
        );
    }
}

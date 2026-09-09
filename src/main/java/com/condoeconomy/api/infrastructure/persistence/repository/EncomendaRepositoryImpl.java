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
        // unidadeId is null since we use string fields for MVP
        
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
    public List<Encomenda> buscarPorMorador(String nome) {
        // Remove suffixos comuns como "(Morador)" para melhorar as chances de match
        String nomeBusca = nome.replaceAll("(?i)\\s*\\(Morador\\)", "").trim();
        
        return springDataRepository.findFlexibleByDestinatario(nomeBusca)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Encomenda toDomain(EncomendaJpaEntity entity) {
        Encomenda encomenda = new Encomenda(
                entity.getId(), 
                entity.getCodigoRastreio(), 
                entity.getTransportadora(), 
                entity.getDestinatario(),
                entity.getUnidadeTexto()
        );
        
        if (StatusEncomenda.RETIRADA.name().equals(entity.getStatus())) {
            encomenda.registrarRetirada();
        }
        return encomenda;
    }
}

package com.condoeconomy.api.infrastructure.persistence.repository.financeiro;

import com.condoeconomy.api.application.gateway.financeiro.BoletoRepository;
import com.condoeconomy.api.domain.entity.financeiro.Boleto;
import com.condoeconomy.api.infrastructure.persistence.entity.financeiro.BoletoJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class BoletoRepositoryImpl implements BoletoRepository {

    private final SpringDataBoletoRepository db;

    public BoletoRepositoryImpl(SpringDataBoletoRepository db) {
        this.db = db;
    }

    @Override
    public List<Boleto> buscarPorUnidade(String unidadeTexto) {
        return db.findByUnidadeTextoContainingIgnoreCaseOrderByDataVencimentoDesc(unidadeTexto)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Boleto> buscarTodos() {
        return db.findAllByOrderByDataVencimentoDesc()
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Boleto> buscarPorId(UUID id) {
        return db.findById(id).map(this::toDomain);
    }

    @Override
    public Boleto salvar(Boleto boleto) {
        return toDomain(db.save(toJpa(boleto)));
    }

    private Boleto toDomain(BoletoJpaEntity e) {
        return new Boleto(e.getId(), e.getUnidadeTexto(), e.getMoradorResponsavel(), e.getCompetencia(),
                e.getValor(), e.getDataVencimento(), e.getDataPagamento(), 
                Boleto.StatusBoleto.valueOf(e.getStatus()), e.getLinhaDigitavel(), e.getUrlPdf());
    }

    private BoletoJpaEntity toJpa(Boleto d) {
        return new BoletoJpaEntity(d.getId(), d.getUnidadeTexto(), d.getMoradorResponsavel(), d.getCompetencia(),
                d.getValor(), d.getDataVencimento(), d.getDataPagamento(), 
                d.getStatus().name(), d.getLinhaDigitavel(), d.getUrlPdf());
    }
}

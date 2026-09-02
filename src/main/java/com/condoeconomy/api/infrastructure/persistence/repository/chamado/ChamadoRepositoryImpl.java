package com.condoeconomy.api.infrastructure.persistence.repository.chamado;

import com.condoeconomy.api.application.gateway.chamado.ChamadoRepository;
import com.condoeconomy.api.domain.entity.chamado.Chamado;
import com.condoeconomy.api.infrastructure.persistence.entity.chamado.ChamadoJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ChamadoRepositoryImpl implements ChamadoRepository {

    private final SpringDataChamadoRepository db;

    public ChamadoRepositoryImpl(SpringDataChamadoRepository db) {
        this.db = db;
    }

    @Override
    public List<Chamado> buscarTodos() {
        return db.findAllByOrderByDataAberturaDesc()
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Chamado> buscarPorId(UUID id) {
        return db.findById(id).map(this::toDomain);
    }

    @Override
    public Chamado salvar(Chamado chamado) {
        return toDomain(db.save(toJpa(chamado)));
    }

    private Chamado toDomain(ChamadoJpaEntity e) {
        return new Chamado(e.getId(), e.getUnidadeTexto(), e.getMoradorSolicitante(), 
                Chamado.CategoriaChamado.valueOf(e.getCategoria()), e.getAssunto(), e.getDescricao(), 
                Chamado.StatusChamado.valueOf(e.getStatus()), e.getDataAbertura(), e.getDataResolucao());
    }

    private ChamadoJpaEntity toJpa(Chamado d) {
        return new ChamadoJpaEntity(d.getId(), d.getUnidadeTexto(), d.getMoradorSolicitante(), 
                d.getCategoria().name(), d.getAssunto(), d.getDescricao(), 
                d.getStatus().name(), d.getDataAbertura(), d.getDataResolucao());
    }
}

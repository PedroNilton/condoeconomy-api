package com.condoeconomy.api.infrastructure.persistence.repository.reserva;

import com.condoeconomy.api.application.gateway.reserva.ReservaRepository;
import com.condoeconomy.api.domain.entity.reserva.AreaComum;
import com.condoeconomy.api.domain.entity.reserva.Convidado;
import com.condoeconomy.api.domain.entity.reserva.Reserva;
import com.condoeconomy.api.infrastructure.persistence.entity.reserva.AreaComumJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.reserva.ConvidadoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.reserva.ReservaJpaEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ReservaRepositoryImpl implements ReservaRepository {

    private final SpringDataReservaRepository reservaDb;
    private final SpringDataAreaComumRepository areaComumDb;

    public ReservaRepositoryImpl(SpringDataReservaRepository reservaDb, SpringDataAreaComumRepository areaComumDb) {
        this.reservaDb = reservaDb;
        this.areaComumDb = areaComumDb;
    }

    @Override
    public Reserva salvar(Reserva reserva) {
        ReservaJpaEntity jpaEntity = toJpa(reserva);
        return toDomain(reservaDb.save(jpaEntity));
    }

    @Override
    public List<Reserva> buscarPorData(LocalDate data) {
        return reservaDb.findByDataReservaOrderByHoraInicioAsc(data)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Reserva> buscarPorStatus(String status) {
        return reservaDb.findByStatusOrderByDataReservaAscHoraInicioAsc(status)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Reserva> buscarPorDataEStatus(LocalDate data, String status) {
        return reservaDb.findByDataReservaAndStatusOrderByHoraInicioAsc(data, status)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Reserva> buscarPorUnidade(String unidade) {
        return reservaDb.findByUnidadeTextoOrderByDataReservaDesc(unidade)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Reserva> buscarPorId(UUID id) {
        return reservaDb.findById(id).map(this::toDomain);
    }

    @Override
    public List<AreaComum> buscarAreasComuns() {
        return areaComumDb.findAll().stream().map(this::toDomainArea).collect(Collectors.toList());
    }

    @Override
    public Optional<AreaComum> buscarAreaComumPorId(UUID id) {
        return areaComumDb.findById(id).map(this::toDomainArea);
    }

    @Override
    public boolean existeReservaNoPeriodo(UUID areaComumId, LocalDate data) {
        return reservaDb.existsByAreaComumIdAndDataReservaAndStatusNot(areaComumId, data, Reserva.StatusReserva.CANCELADA.name());
    }

    // Mappers
    private AreaComum toDomainArea(AreaComumJpaEntity e) {
        return new AreaComum(e.getId(), e.getNome(), e.getDescricao(), e.getCapacidadeMaxima(), e.getTaxaLocacao());
    }

    private AreaComumJpaEntity toJpaArea(AreaComum d) {
        return new AreaComumJpaEntity(d.getId(), d.getNome(), d.getDescricao(), d.getCapacidadeMaxima(), d.getTaxaLocacao());
    }

    private Reserva toDomain(ReservaJpaEntity e) {
        List<Convidado> convidados = e.getConvidados().stream().map(c -> new Convidado(
                c.getId(), c.getNome(), c.getDocumento(), 
                Convidado.StatusEntrada.valueOf(c.getStatusEntrada()), c.getHoraEntrada()
        )).collect(Collectors.toList());

        return new Reserva(e.getId(), toDomainArea(e.getAreaComum()), e.getUnidadeTexto(), e.getMoradorSolicitante(),
                e.getTitulo(), e.getDataReserva(), e.getHoraInicio(), e.getHoraFim(), 
                Reserva.StatusReserva.valueOf(e.getStatus()), e.getDataSolicitacao(), convidados, e.getMotivoRejeicao());
    }

    private ReservaJpaEntity toJpa(Reserva d) {
        ReservaJpaEntity e = new ReservaJpaEntity();
        e.setId(d.getId());
        e.setAreaComum(toJpaArea(d.getAreaComum()));
        e.setUnidadeTexto(d.getUnidadeTexto());
        e.setMoradorSolicitante(d.getMoradorSolicitante());
        e.setTitulo(d.getTitulo());
        e.setDataReserva(d.getDataReserva());
        e.setHoraInicio(d.getHoraInicio());
        e.setHoraFim(d.getHoraFim());
        e.setStatus(d.getStatus().name());
        e.setDataSolicitacao(d.getDataSolicitacao());
        e.setMotivoRejeicao(d.getMotivoRejeicao());

        List<ConvidadoJpaEntity> convidados = d.getConvidados().stream().map(c -> new ConvidadoJpaEntity(
                c.getId(), e, c.getNome(), c.getDocumento(), c.getStatusEntrada().name(), c.getHoraEntrada()
        )).collect(Collectors.toList());
        e.setConvidados(convidados);

        return e;
    }
}

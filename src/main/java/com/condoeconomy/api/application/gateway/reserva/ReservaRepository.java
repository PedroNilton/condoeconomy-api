package com.condoeconomy.api.application.gateway.reserva;

import com.condoeconomy.api.domain.entity.reserva.AreaComum;
import com.condoeconomy.api.domain.entity.reserva.Reserva;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservaRepository {
    Reserva salvar(Reserva reserva);
    List<Reserva> buscarPorData(LocalDate data);
    Optional<Reserva> buscarPorId(UUID id);
    List<AreaComum> buscarAreasComuns();
    Optional<AreaComum> buscarAreaComumPorId(UUID id);
    boolean existeReservaNoPeriodo(UUID areaComumId, LocalDate data);
}

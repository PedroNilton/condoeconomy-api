package com.condoeconomy.api.application.usecase.reserva;

import com.condoeconomy.api.domain.entity.reserva.Reserva;
import com.condoeconomy.api.application.gateway.reserva.ReservaRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AprovarRejeitarReservaUseCase {
    private final ReservaRepository repository;

    public AprovarRejeitarReservaUseCase(ReservaRepository repository) {
        this.repository = repository;
    }

    public void aprovar(UUID reservaId) {
        Reserva reserva = repository.buscarPorId(reservaId)
            .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));
        reserva.aprovar();
        repository.salvar(reserva);
    }

    public void rejeitar(UUID reservaId) {
        Reserva reserva = repository.buscarPorId(reservaId)
            .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));
        reserva.rejeitar();
        repository.salvar(reserva);
    }
}

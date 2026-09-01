package com.condoeconomy.api.application.usecase.reserva;

import com.condoeconomy.api.application.gateway.reserva.ReservaRepository;
import com.condoeconomy.api.domain.entity.reserva.Convidado;
import com.condoeconomy.api.domain.entity.reserva.Reserva;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CheckInConvidadoUseCase {
    private final ReservaRepository reservaRepository;

    public CheckInConvidadoUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public Reserva executar(UUID reservaId, UUID convidadoId) {
        Reserva reserva = reservaRepository.buscarPorId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));

        Convidado convidado = reserva.getConvidados().stream()
                .filter(c -> c.getId().equals(convidadoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Convidado não pertence a esta reserva"));

        convidado.registrarEntrada();
        return reservaRepository.salvar(reserva);
    }
}

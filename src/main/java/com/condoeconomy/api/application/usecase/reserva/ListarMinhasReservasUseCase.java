package com.condoeconomy.api.application.usecase.reserva;

import com.condoeconomy.api.application.gateway.reserva.ReservaRepository;
import com.condoeconomy.api.domain.entity.reserva.Reserva;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarMinhasReservasUseCase {
    private final ReservaRepository reservaRepository;

    public ListarMinhasReservasUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public List<Reserva> executar(String unidade) {
        return reservaRepository.buscarPorUnidade(unidade);
    }
}
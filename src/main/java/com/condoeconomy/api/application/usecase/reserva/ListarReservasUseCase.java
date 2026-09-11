package com.condoeconomy.api.application.usecase.reserva;

import com.condoeconomy.api.application.gateway.reserva.ReservaRepository;
import com.condoeconomy.api.domain.entity.reserva.Reserva;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ListarReservasUseCase {
    private final ReservaRepository reservaRepository;

    public ListarReservasUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public List<Reserva> executar(LocalDate data, String status) {
        if (data != null && status != null) {
            return reservaRepository.buscarPorDataEStatus(data, status);
        } else if (data != null) {
            return reservaRepository.buscarPorData(data);
        } else if (status != null) {
            return reservaRepository.buscarPorStatus(status);
        }
        throw new IllegalArgumentException("Forneça data ou status para buscar reservas");
    }
}

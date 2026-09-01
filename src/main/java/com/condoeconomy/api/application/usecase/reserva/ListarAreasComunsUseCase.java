package com.condoeconomy.api.application.usecase.reserva;

import com.condoeconomy.api.application.gateway.reserva.ReservaRepository;
import com.condoeconomy.api.domain.entity.reserva.AreaComum;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarAreasComunsUseCase {
    private final ReservaRepository reservaRepository;

    public ListarAreasComunsUseCase(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    public List<AreaComum> executar() {
        return reservaRepository.buscarAreasComuns();
    }
}

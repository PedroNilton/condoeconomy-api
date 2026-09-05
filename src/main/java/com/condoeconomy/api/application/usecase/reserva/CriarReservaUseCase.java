package com.condoeconomy.api.application.usecase.reserva;

import com.condoeconomy.api.application.gateway.reserva.ReservaRepository;
import com.condoeconomy.api.application.service.NotificationService;
import com.condoeconomy.api.domain.entity.reserva.AreaComum;
import com.condoeconomy.api.domain.entity.reserva.Convidado;
import com.condoeconomy.api.domain.entity.reserva.Reserva;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CriarReservaUseCase {

    private final ReservaRepository reservaRepository;
    private final NotificationService notificationService;

    public CriarReservaUseCase(ReservaRepository reservaRepository, NotificationService notificationService) {
        this.reservaRepository = reservaRepository;
        this.notificationService = notificationService;
    }

    public Reserva executar(UUID areaComumId, String unidade, String morador, String titulo, 
                            LocalDate data, LocalTime inicio, LocalTime fim, List<ConvidadoDtoInput> convidadosInput) {
        
        if (reservaRepository.existeReservaNoPeriodo(areaComumId, data)) {
            throw new IllegalArgumentException("Já existe uma reserva para esta área comum nesta data.");
        }

        AreaComum area = reservaRepository.buscarAreaComumPorId(areaComumId)
                .orElseThrow(() -> new IllegalArgumentException("Área comum não encontrada"));

        List<Convidado> convidados = convidadosInput.stream()
                .map(c -> Convidado.criar(c.nome(), c.documento()))
                .collect(Collectors.toList());

        Reserva reserva = Reserva.criar(area, unidade, morador, titulo, data, inicio, fim, convidados);
        Reserva salva = reservaRepository.salvar(reserva);
        notificationService.notifyReservasUpdate();
        return salva;
    }

    public record ConvidadoDtoInput(String nome, String documento) {}
}

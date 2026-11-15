package com.condoeconomy.api.application.usecase.reserva;

import com.condoeconomy.api.domain.entity.reserva.Reserva;
import com.condoeconomy.api.application.gateway.reserva.ReservaRepository;
import com.condoeconomy.api.application.service.NotificationService;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AprovarRejeitarReservaUseCase {
    private final ReservaRepository repository;
    private final NotificationService notificationService;

    public AprovarRejeitarReservaUseCase(ReservaRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public void aprovar(UUID reservaId) {
        Reserva reserva = repository.buscarPorId(reservaId)
            .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));

        boolean conflito = repository.buscarPorDataEStatus(
                reserva.getDataReserva(),
                Reserva.StatusReserva.APROVADA.name()
        ).stream().anyMatch(outra ->
                !outra.getId().equals(reserva.getId())
                && outra.getAreaComum().getId().equals(reserva.getAreaComum().getId())
                && outra.getHoraInicio().isBefore(reserva.getHoraFim())
                && reserva.getHoraInicio().isBefore(outra.getHoraFim())
        );

        if (conflito) {
            throw new IllegalStateException("Já existe uma reserva aprovada para esta área comum no mesmo dia e horário.");
        }

        reserva.aprovar();
        repository.salvar(reserva);
        notificationService.notifyReservasUpdate();
    }

    public void rejeitar(UUID reservaId, String motivo) {
        Reserva reserva = repository.buscarPorId(reservaId)
            .orElseThrow(() -> new RuntimeException("Reserva não encontrada"));
        reserva.rejeitar(motivo);
        repository.salvar(reserva);
        notificationService.notifyReservasUpdate();
    }
}

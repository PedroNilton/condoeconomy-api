package com.condoeconomy.api.application.usecase.reserva;

import com.condoeconomy.api.application.gateway.reserva.ReservaRepository;
import com.condoeconomy.api.application.service.NotificationService;
import com.condoeconomy.api.domain.entity.reserva.Convidado;
import com.condoeconomy.api.domain.entity.reserva.Reserva;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CheckInConvidadoUseCase {
    private final ReservaRepository reservaRepository;
    private final NotificationService notificationService;

    public CheckInConvidadoUseCase(ReservaRepository reservaRepository, NotificationService notificationService) {
        this.reservaRepository = reservaRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Reserva executar(UUID reservaId, UUID convidadoId) {
        Reserva reserva = reservaRepository.buscarPorId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));

        Convidado convidado = reserva.getConvidados().stream()
                .filter(c -> c.getId().equals(convidadoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Convidado não pertence a esta reserva"));

        convidado.registrarEntrada();
        Reserva salva = reservaRepository.salvar(reserva);
        notificationService.notifyReservasUpdate();
        return salva;
    }

    @Transactional
    public Reserva alternarEntrada(UUID reservaId, UUID convidadoId) {
        Reserva reserva = reservaRepository.buscarPorId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));

        Convidado convidado = reserva.getConvidados().stream()
                .filter(c -> c.getId().equals(convidadoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Convidado não pertence a esta reserva"));

        convidado.alternarEntrada();
        Reserva salva = reservaRepository.salvar(reserva);
        notificationService.notifyReservasUpdate();
        return salva;
    }

    @Transactional
    public Reserva adicionarConvidadoPortaria(UUID reservaId, String nome) {
        Reserva reserva = reservaRepository.buscarPorId(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada"));

        Convidado novo = Convidado.criar(nome, "PORTARIA");
        reserva.getConvidados().add(novo);
        Reserva salva = reservaRepository.salvar(reserva);
        notificationService.notifyReservasUpdate();
        return salva;
    }
}

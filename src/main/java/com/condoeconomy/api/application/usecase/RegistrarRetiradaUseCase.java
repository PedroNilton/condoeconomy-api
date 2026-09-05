package com.condoeconomy.api.application.usecase;

import com.condoeconomy.api.application.gateway.EncomendaRepository;
import com.condoeconomy.api.application.service.NotificationService;
import com.condoeconomy.api.domain.entity.Encomenda;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegistrarRetiradaUseCase {

    private final EncomendaRepository encomendaRepository;
    private final NotificationService notificationService;

    public RegistrarRetiradaUseCase(EncomendaRepository encomendaRepository, NotificationService notificationService) {
        this.encomendaRepository = encomendaRepository;
        this.notificationService = notificationService;
    }

    public Encomenda executar(UUID encomendaId) {
        Encomenda encomenda = encomendaRepository.buscarPorId(encomendaId)
                .orElseThrow(() -> new IllegalArgumentException("Encomenda não encontrada"));

        encomenda.registrarRetirada();
        Encomenda salva = encomendaRepository.salvar(encomenda);
        notificationService.notifyEncomendasUpdate();
        return salva;
    }
}

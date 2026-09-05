package com.condoeconomy.api.application.usecase.visitante;

import com.condoeconomy.api.domain.entity.visitante.Visitante;
import com.condoeconomy.api.infrastructure.repository.visitante.VisitanteRepository;
import com.condoeconomy.api.application.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AlterarStatusVisitanteUseCase {

    private final VisitanteRepository repository;
    private final NotificationService notificationService;

    public AlterarStatusVisitanteUseCase(VisitanteRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Visitante registrarEntrada(UUID id) {
        Visitante visitante = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visitante não encontrado"));
        visitante.registrarEntrada();
        Visitante salvo = repository.save(visitante);
        notificationService.notifyVisitantesUpdate();
        return salvo;
    }

    @Transactional
    public Visitante registrarSaida(UUID id) {
        Visitante visitante = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visitante não encontrado"));
        visitante.registrarSaida();
        Visitante salvo = repository.save(visitante);
        notificationService.notifyVisitantesUpdate();
        return salvo;
    }
}

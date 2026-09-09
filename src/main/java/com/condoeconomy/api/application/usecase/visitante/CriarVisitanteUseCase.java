package com.condoeconomy.api.application.usecase.visitante;

import com.condoeconomy.api.domain.entity.visitante.Visitante;
import com.condoeconomy.api.infrastructure.repository.visitante.VisitanteRepository;
import com.condoeconomy.api.application.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CriarVisitanteUseCase {

    private final VisitanteRepository repository;
    private final NotificationService notificationService;

    public CriarVisitanteUseCase(VisitanteRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Visitante executar(String nome, String sobrenome, String blocoDestino, String unidadeDestino, String placaVeiculo) {
        // Auto-Checkin flow fields
        Visitante visitante = new Visitante(nome, sobrenome, null, LocalDate.now(), blocoDestino, unidadeDestino, null, placaVeiculo, "VISITANTE");
        Visitante salvo = repository.save(visitante);
        notificationService.notifyVisitantesUpdate();
        return salvo;
    }
}

package com.condoeconomy.api.application.usecase.visitante;

import com.condoeconomy.api.application.service.NotificationService;
import com.condoeconomy.api.domain.entity.visitante.Visitante;
import com.condoeconomy.api.infrastructure.repository.visitante.VisitanteRepository;
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
        return executar(nome, sobrenome, blocoDestino, unidadeDestino, placaVeiculo, LocalDate.now(), "VISITANTE", "A CONFIRMAR");
    }

    @Transactional
    public Visitante executar(String nome,
                              String sobrenome,
                              String blocoDestino,
                              String unidadeDestino,
                              String placaVeiculo,
                              LocalDate dataVisita,
                              String tipo,
                              String moradorResponsavel) {
        Visitante visitante = new Visitante(
                nome,
                sobrenome,
                "NÃO INFORMADO",
                dataVisita != null ? dataVisita : LocalDate.now(),
                blocoDestino,
                unidadeDestino,
                moradorResponsavel != null && !moradorResponsavel.isBlank() ? moradorResponsavel : "A CONFIRMAR",
                placaVeiculo,
                tipo != null && !tipo.isBlank() ? tipo.toUpperCase() : "VISITANTE"
        );
        Visitante salvo = repository.save(visitante);
        notificationService.notifyVisitantesUpdate();
        return salvo;
    }
}

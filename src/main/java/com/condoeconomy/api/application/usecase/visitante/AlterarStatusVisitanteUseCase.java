package com.condoeconomy.api.application.usecase.visitante;

import com.condoeconomy.api.domain.entity.visitante.Visitante;
import com.condoeconomy.api.infrastructure.repository.visitante.VisitanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AlterarStatusVisitanteUseCase {

    private final VisitanteRepository repository;

    public AlterarStatusVisitanteUseCase(VisitanteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Visitante registrarEntrada(UUID id) {
        Visitante visitante = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visitante não encontrado"));
        visitante.registrarEntrada();
        return repository.save(visitante);
    }

    @Transactional
    public Visitante registrarSaida(UUID id) {
        Visitante visitante = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Visitante não encontrado"));
        visitante.registrarSaida();
        return repository.save(visitante);
    }
}

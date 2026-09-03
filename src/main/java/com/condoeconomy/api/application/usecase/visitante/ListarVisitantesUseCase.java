package com.condoeconomy.api.application.usecase.visitante;

import com.condoeconomy.api.domain.entity.visitante.Visitante;
import com.condoeconomy.api.infrastructure.repository.visitante.VisitanteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ListarVisitantesUseCase {

    private final VisitanteRepository repository;

    public ListarVisitantesUseCase(VisitanteRepository repository) {
        this.repository = repository;
    }

    public List<Visitante> executar(String unidadeDestino, LocalDate dataVisita) {
        if (unidadeDestino != null && !unidadeDestino.isEmpty()) {
            return repository.findByUnidadeDestinoOrderByDataVisitaDesc(unidadeDestino);
        }
        return repository.findByDataVisitaOrderByNomeAsc(dataVisita != null ? dataVisita : LocalDate.now());
    }
}

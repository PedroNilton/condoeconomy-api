package com.condoeconomy.api.infrastructure.repository.visitante;

import com.condoeconomy.api.domain.entity.visitante.Visitante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface VisitanteRepository extends JpaRepository<Visitante, UUID> {
    List<Visitante> findByDataVisitaOrderByNomeAsc(LocalDate dataVisita);
    List<Visitante> findByUnidadeDestinoOrderByDataVisitaDesc(String unidadeDestino);
}

package com.condoeconomy.api.infrastructure.persistence.repository.reserva;

import com.condoeconomy.api.infrastructure.persistence.entity.reserva.ReservaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SpringDataReservaRepository extends JpaRepository<ReservaJpaEntity, UUID> {
    List<ReservaJpaEntity> findByDataReservaOrderByHoraInicioAsc(LocalDate data);
    List<ReservaJpaEntity> findByStatusOrderByDataReservaAscHoraInicioAsc(String status);
    List<ReservaJpaEntity> findByDataReservaAndStatusOrderByHoraInicioAsc(LocalDate data, String status);
    List<ReservaJpaEntity> findByUnidadeTextoOrderByDataReservaDesc(String unidadeTexto);
    boolean existsByAreaComumIdAndDataReservaAndStatusNot(UUID areaComumId, LocalDate data, String status);
}

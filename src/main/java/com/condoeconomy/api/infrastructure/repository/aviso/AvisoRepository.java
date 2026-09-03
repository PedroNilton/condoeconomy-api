package com.condoeconomy.api.infrastructure.repository.aviso;

import com.condoeconomy.api.domain.entity.aviso.Aviso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AvisoRepository extends JpaRepository<Aviso, String> {
    List<Aviso> findAllByOrderByDataCriacaoDesc();
}

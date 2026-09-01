package com.condoeconomy.api.application.gateway;

import com.condoeconomy.api.domain.entity.Encomenda;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EncomendaRepository {
    Encomenda salvar(Encomenda encomenda);
    Optional<Encomenda> buscarPorId(UUID id);
    List<Encomenda> buscarTodas();
}

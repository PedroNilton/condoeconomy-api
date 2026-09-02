package com.condoeconomy.api.application.gateway.chamado;

import com.condoeconomy.api.domain.entity.chamado.Chamado;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChamadoRepository {
    List<Chamado> buscarTodos();
    Optional<Chamado> buscarPorId(UUID id);
    Chamado salvar(Chamado chamado);
}

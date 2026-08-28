package com.condoeconomy.api.application.gateway;

import com.condoeconomy.api.domain.entity.Unidade;
import java.util.Optional;
import java.util.UUID;

public interface UnidadeRepository {
    Optional<Unidade> buscarPorId(UUID id);
}

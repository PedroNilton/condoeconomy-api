package com.condoeconomy.api.application.gateway;

import com.condoeconomy.api.domain.entity.Produto;
import java.util.Optional;
import java.util.UUID;

public interface ProdutoRepository {
    Optional<Produto> buscarPorId(UUID id);
    Produto salvar(Produto produto);
}

package com.condoeconomy.api.application.gateway.financeiro;

import com.condoeconomy.api.domain.entity.financeiro.Boleto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoletoRepository {
    List<Boleto> buscarPorUnidade(String unidadeTexto);
    List<Boleto> buscarTodos();
    Optional<Boleto> buscarPorId(UUID id);
    Boleto salvar(Boleto boleto);
}

package com.condoeconomy.api.application.usecase;

import com.condoeconomy.api.application.gateway.EncomendaRepository;
import com.condoeconomy.api.domain.entity.Encomenda;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegistrarRetiradaUseCase {

    private final EncomendaRepository encomendaRepository;

    public RegistrarRetiradaUseCase(EncomendaRepository encomendaRepository) {
        this.encomendaRepository = encomendaRepository;
    }

    public Encomenda executar(UUID encomendaId) {
        Encomenda encomenda = encomendaRepository.buscarPorId(encomendaId)
                .orElseThrow(() -> new IllegalArgumentException("Encomenda não encontrada"));

        encomenda.registrarRetirada();
        return encomendaRepository.salvar(encomenda);
    }
}

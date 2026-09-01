package com.condoeconomy.api.application.usecase.financeiro;

import com.condoeconomy.api.application.gateway.financeiro.BoletoRepository;
import com.condoeconomy.api.domain.entity.financeiro.Boleto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class DarBaixaBoletoUseCase {
    private final BoletoRepository boletoRepository;

    public DarBaixaBoletoUseCase(BoletoRepository boletoRepository) {
        this.boletoRepository = boletoRepository;
    }

    public Boleto executar(UUID boletoId) {
        Boleto boleto = boletoRepository.buscarPorId(boletoId)
                .orElseThrow(() -> new IllegalArgumentException("Boleto não encontrado"));
        
        boleto.registrarPagamento(LocalDate.now());
        return boletoRepository.salvar(boleto);
    }
}

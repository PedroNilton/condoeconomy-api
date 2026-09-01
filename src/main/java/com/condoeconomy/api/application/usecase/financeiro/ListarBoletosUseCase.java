package com.condoeconomy.api.application.usecase.financeiro;

import com.condoeconomy.api.application.gateway.financeiro.BoletoRepository;
import com.condoeconomy.api.domain.entity.financeiro.Boleto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarBoletosUseCase {
    private final BoletoRepository boletoRepository;

    public ListarBoletosUseCase(BoletoRepository boletoRepository) {
        this.boletoRepository = boletoRepository;
    }

    public List<Boleto> executar(String unidadeTexto) {
        List<Boleto> boletos;
        if (unidadeTexto != null && !unidadeTexto.isBlank()) {
            boletos = boletoRepository.buscarPorUnidade(unidadeTexto);
        } else {
            boletos = boletoRepository.buscarTodos();
        }
        
        // Atualiza os status baseados na data atual (se venceram)
        boletos.forEach(Boleto::atualizarStatus);
        
        return boletos;
    }
}

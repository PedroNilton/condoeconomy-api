package com.condoeconomy.api.application.usecase.chamado;

import com.condoeconomy.api.application.gateway.chamado.ChamadoRepository;
import com.condoeconomy.api.domain.entity.chamado.Chamado;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarChamadosUseCase {
    private final ChamadoRepository chamadoRepository;

    public ListarChamadosUseCase(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    public List<Chamado> executar() {
        return chamadoRepository.buscarTodos();
    }
}

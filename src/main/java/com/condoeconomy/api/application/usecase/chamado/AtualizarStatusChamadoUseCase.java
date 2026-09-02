package com.condoeconomy.api.application.usecase.chamado;

import com.condoeconomy.api.application.gateway.chamado.ChamadoRepository;
import com.condoeconomy.api.domain.entity.chamado.Chamado;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AtualizarStatusChamadoUseCase {
    private final ChamadoRepository chamadoRepository;

    public AtualizarStatusChamadoUseCase(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    public Chamado executar(UUID id, Chamado.StatusChamado novoStatus) {
        Chamado chamado = chamadoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado"));
        
        if (novoStatus == Chamado.StatusChamado.EM_ANDAMENTO) {
            chamado.iniciarAtendimento();
        } else if (novoStatus == Chamado.StatusChamado.RESOLVIDO) {
            chamado.resolver();
        }

        return chamadoRepository.salvar(chamado);
    }
}

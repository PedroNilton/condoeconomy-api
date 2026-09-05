package com.condoeconomy.api.application.usecase.chamado;

import com.condoeconomy.api.application.gateway.chamado.ChamadoRepository;
import com.condoeconomy.api.application.service.NotificationService;
import com.condoeconomy.api.domain.entity.chamado.Chamado;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AtualizarStatusChamadoUseCase {
    private final ChamadoRepository chamadoRepository;
    private final NotificationService notificationService;

    public AtualizarStatusChamadoUseCase(ChamadoRepository chamadoRepository, NotificationService notificationService) {
        this.chamadoRepository = chamadoRepository;
        this.notificationService = notificationService;
    }

    public Chamado executar(UUID id, Chamado.StatusChamado novoStatus) {
        Chamado chamado = chamadoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado"));
        
        if (novoStatus == Chamado.StatusChamado.EM_ANDAMENTO) {
            chamado.iniciarAtendimento();
        } else if (novoStatus == Chamado.StatusChamado.RESOLVIDO) {
            chamado.resolver();
        }

        Chamado salvo = chamadoRepository.salvar(chamado);
        notificationService.notifyChamadosUpdate();
        return salvo;
    }
}

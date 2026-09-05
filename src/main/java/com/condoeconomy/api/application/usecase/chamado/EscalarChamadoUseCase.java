package com.condoeconomy.api.application.usecase.chamado;

import com.condoeconomy.api.domain.entity.chamado.Chamado;
import com.condoeconomy.api.application.gateway.chamado.ChamadoRepository;
import com.condoeconomy.api.application.service.NotificationService;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class EscalarChamadoUseCase {
    private final ChamadoRepository repository;
    private final NotificationService notificationService;

    public EscalarChamadoUseCase(ChamadoRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    public void execute(UUID chamadoId) {
        Chamado chamado = repository.buscarPorId(chamadoId)
            .orElseThrow(() -> new RuntimeException("Chamado não encontrado"));
        chamado.escalarParaSindico();
        repository.salvar(chamado);
        notificationService.notifyChamadosUpdate();
    }
}

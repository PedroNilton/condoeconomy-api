package com.condoeconomy.api.application.usecase;

import com.condoeconomy.api.application.gateway.EncomendaRepository;
import com.condoeconomy.api.application.gateway.NotificationGateway;
import com.condoeconomy.api.application.service.NotificationService;
import com.condoeconomy.api.domain.entity.Encomenda;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReceberEncomendaUseCase {

    private final EncomendaRepository encomendaRepository;
    private final NotificationGateway notificationGateway;
    private final NotificationService notificationService;

    public ReceberEncomendaUseCase(EncomendaRepository encomendaRepository, 
                                   NotificationGateway notificationGateway,
                                   NotificationService notificationService) {
        this.encomendaRepository = encomendaRepository;
        this.notificationGateway = notificationGateway;
        this.notificationService = notificationService;
    }

    public Encomenda executar(String codigoRastreio, String transportadora, String destinatario, String unidade) {
        Encomenda novaEncomenda = new Encomenda(UUID.randomUUID(), codigoRastreio, transportadora, destinatario, unidade);
        Encomenda encomendaSalva = encomendaRepository.salvar(novaEncomenda);

        String titulo = "Nova encomenda chegou!";
        String mensagem = String.format("Um pacote da %s para %s está aguardando retirada na portaria.", transportadora, destinatario);
        notificationGateway.notificarMorador(destinatario, unidade, titulo, mensagem);

        notificationService.notifyEncomendasUpdate();
        return encomendaSalva;
    }
}

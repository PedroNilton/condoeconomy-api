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
        
        // 1. Cria a encomenda de acordo com as regras de dominio (ja nasce AGUARDANDO_RETIRADA)
        Encomenda novaEncomenda = new Encomenda(UUID.randomUUID(), codigoRastreio, transportadora, destinatario, unidade);

        // 2. Persiste no banco de dados
        Encomenda encomendaSalva = encomendaRepository.salvar(novaEncomenda);

        // 3. Notificacao
        String titulo = "Nova encomenda chegou!";
        String mensagem = String.format("Um pacote da %s para %s esta aguardando retirada na portaria.", transportadora, destinatario);
        notificationGateway.notificarMoradoresDaUnidade(null, titulo, mensagem);

        // 4. Websocket
        notificationService.notifyEncomendasUpdate();

        return encomendaSalva;
    }
}

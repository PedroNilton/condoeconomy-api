package com.condoeconomy.api.infrastructure.notification;

import com.condoeconomy.api.application.gateway.NotificationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationGatewayImpl implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGatewayImpl.class);

    @Override
    public void notificarMoradoresDaUnidade(UUID unidadeId, String titulo, String mensagem) {
        // Na vida real, integraríamos com Firebase Cloud Messaging (FCM) ou WebSockets
        log.info("[DISPARO DE PUSH NOTIFICATION]");
        log.info("Destino: Unidade ID [{}]", unidadeId);
        log.info("Título: {}", titulo);
        log.info("Mensagem: {}", mensagem);
    }
}

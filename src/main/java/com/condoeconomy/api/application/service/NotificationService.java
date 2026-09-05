package com.condoeconomy.api.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyChamadosUpdate() {
        messagingTemplate.convertAndSend("/topic/chamados", "UPDATE");
    }

    public void notifyReservasUpdate() {
        messagingTemplate.convertAndSend("/topic/reservas", "UPDATE");
    }

    public void notifyVisitantesUpdate() {
        messagingTemplate.convertAndSend("/topic/visitantes", "UPDATE");
    }

    public void notifyEncomendasUpdate() {
        messagingTemplate.convertAndSend("/topic/encomendas", "UPDATE");
    }
}

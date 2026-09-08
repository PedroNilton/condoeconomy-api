package com.condoeconomy.api.application.service.notification;

public interface PushNotificationService {
    void enviarPush(String tokenDispositivo, String titulo, String corpo);
}

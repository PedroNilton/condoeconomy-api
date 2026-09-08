package com.condoeconomy.api.application.service.notification;

public interface EmailService {
    void enviarEmail(String destinatario, String assunto, String corpoHtml);
}

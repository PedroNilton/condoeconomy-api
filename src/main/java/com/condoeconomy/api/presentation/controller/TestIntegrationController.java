package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.service.notification.EmailService;
import com.condoeconomy.api.application.service.notification.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/testes")
public class TestIntegrationController {

    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;

    public TestIntegrationController(EmailService emailService, PushNotificationService pushNotificationService) {
        this.emailService = emailService;
        this.pushNotificationService = pushNotificationService;
    }

    @GetMapping("/notificacoes")
    public ResponseEntity<String> testarNotificacoes() {
        
        System.out.println("--- INICIANDO TESTE DE NOTIFICAÇÕES ---");
        
        // 1. Testa E-mail
        emailService.enviarEmail(
                "carlos.silva@email.com", 
                "CondoEconomy - Teste de E-mail", 
                "<h1>Sua encomenda chegou!</h1><p>Por favor, retire na portaria.</p>"
        );
        
        // 2. Testa Push Notification
        pushNotificationService.enviarPush(
                "token_celular_do_carlos_12345", 
                "Encomenda na Portaria", 
                "Você tem uma nova encomenda esperando na portaria."
        );
        
        System.out.println("--- TESTE FINALIZADO ---");
        
        return ResponseEntity.ok("Teste de E-mail e Push enviado para o terminal do Servidor com sucesso!");
    }
}

package com.condoeconomy.api.presentation.controller;

import com.condoeconomy.api.application.service.notification.EmailService;
import com.condoeconomy.api.application.service.notification.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/testes")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class TestIntegrationController {

    private static final Logger log = LoggerFactory.getLogger(TestIntegrationController.class);

    private final EmailService emailService;
    private final PushNotificationService pushNotificationService;

    public TestIntegrationController(EmailService emailService, PushNotificationService pushNotificationService) {
        this.emailService = emailService;
        this.pushNotificationService = pushNotificationService;
    }

    @GetMapping("/notificacoes")
    public ResponseEntity<String> testarNotificacoes() {
        log.info("[DIAGNÓSTICO DE INTEGRAÇÃO] Iniciando disparo de teste de E-mail e Push Notification...");

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

        log.info("[DIAGNÓSTICO DE INTEGRAÇÃO] Teste de canais finalizado.");

        return ResponseEntity.ok("Teste de E-mail e Push executado com sucesso! Verifique os logs estruturados do servidor.");
    }
}

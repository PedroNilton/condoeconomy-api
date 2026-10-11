package com.condoeconomy.api.presentation.controller.webhook;

import com.condoeconomy.api.application.usecase.financeiro.DarBaixaBoletoUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/asaas")
public class WebhookAsaasController {

    private final DarBaixaBoletoUseCase darBaixaBoletoUseCase;
    private final String webhookToken;

    public WebhookAsaasController(
            DarBaixaBoletoUseCase darBaixaBoletoUseCase,
            @Value("${asaas.webhook.token:asaas-dev-secret-token}") String webhookToken) {
        this.darBaixaBoletoUseCase = darBaixaBoletoUseCase;
        this.webhookToken = webhookToken;
    }

    /**
     * Rota chamada pelo Gateway de Pagamento quando um cliente paga o boleto/PIX.
     * Protegida contra requisições forjadas validando o cabeçalho 'asaas-access-token'.
     */
    @PostMapping
    public ResponseEntity<Void> receberWebhookPagamento(
            @RequestHeader(value = "asaas-access-token", required = false) String tokenRecebido,
            @RequestBody Map<String, Object> payload) {

        // Impede que terceiros forjem notificações de pagamento para quitar boletos indevidamente
        if (tokenRecebido == null || !tokenRecebido.equals(this.webhookToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String event = (String) payload.get("event");
        
        if ("PAYMENT_RECEIVED".equals(event)) {
            Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
            if (payment != null) {
                String externalReference = (String) payment.get("externalReference");
                if (externalReference != null) {
                    darBaixaBoletoUseCase.executar(UUID.fromString(externalReference));
                }
            }
        }
        
        return ResponseEntity.ok().build();
    }
}

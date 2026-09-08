package com.condoeconomy.api.presentation.controller.webhook;

import com.condoeconomy.api.application.usecase.financeiro.DarBaixaBoletoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/asaas")
public class WebhookAsaasController {

    private final DarBaixaBoletoUseCase darBaixaBoletoUseCase;

    public WebhookAsaasController(DarBaixaBoletoUseCase darBaixaBoletoUseCase) {
        this.darBaixaBoletoUseCase = darBaixaBoletoUseCase;
    }

    /**
     * Rota chamada pelo Gateway de Pagamento quando um cliente paga o boleto/PIX.
     */
    @PostMapping
    public ResponseEntity<Void> receberWebhookPagamento(@RequestBody Map<String, Object> payload) {
        
        System.out.println("Webhook do Asaas recebido: " + payload);
        
        String event = (String) payload.get("event");
        
        if ("PAYMENT_RECEIVED".equals(event)) {
            // Em uma implementação real, o externalReference viria no payload e seria o UUID do nosso boleto
            Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
            String externalReference = (String) payment.get("externalReference");
            
            if (externalReference != null) {
                darBaixaBoletoUseCase.executar(UUID.fromString(externalReference));
                System.out.println("Baixa automática realizada com sucesso para o boleto: " + externalReference);
            }
        }
        
        return ResponseEntity.ok().build();
    }
}

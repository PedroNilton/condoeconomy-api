package com.condoeconomy.api.presentation.controller.webhook;

import com.condoeconomy.api.application.usecase.financeiro.DarBaixaBoletoUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks/asaas")
public class WebhookAsaasController {

    private static final int MAX_EVENTOS_PROCESSADOS = 5_000;

    private final DarBaixaBoletoUseCase darBaixaBoletoUseCase;
    private final String webhookToken;
    private final Set<String> eventosProcessados = Collections.synchronizedSet(
            Collections.newSetFromMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_EVENTOS_PROCESSADOS;
                }
            })
    );

    public WebhookAsaasController(
            DarBaixaBoletoUseCase darBaixaBoletoUseCase,
            @Value("${asaas.webhook.token:asaas-dev-secret-token}") String webhookToken) {
        this.darBaixaBoletoUseCase = darBaixaBoletoUseCase;
        this.webhookToken = webhookToken;
    }

    /**
     * Rota chamada pelo Gateway de Pagamento quando um cliente paga o boleto/PIX.
     * Protegida contra requisições forjadas e timing attacks validando 'asaas-access-token' em tempo constante,
     * além de garantir idempotência contra replay de eventos já processados.
     */
    @PostMapping
    public ResponseEntity<Void> receberWebhookPagamento(
            @RequestHeader(value = "asaas-access-token", required = false) String tokenRecebido,
            @RequestBody Map<String, Object> payload) {

        if (!isTokenValidoTempoConstante(tokenRecebido)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String event = (String) payload.get("event");
        
        if ("PAYMENT_RECEIVED".equals(event)) {
            Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
            if (payment != null) {
                String externalReference = (String) payment.get("externalReference");
                String paymentId = payment.get("id") != null ? String.valueOf(payment.get("id")) : externalReference;
                if (externalReference != null) {
                    String chaveIdempotencia = event + ":" + paymentId;
                    if (!eventosProcessados.add(chaveIdempotencia)) {
                        // Evento duplicado/replay: retorna 200 OK de forma idempotente sem reprocessar baixa
                        return ResponseEntity.ok().build();
                    }
                    darBaixaBoletoUseCase.executar(UUID.fromString(externalReference));
                }
            }
        }
        
        return ResponseEntity.ok().build();
    }

    private boolean isTokenValidoTempoConstante(String tokenRecebido) {
        if (tokenRecebido == null || tokenRecebido.isBlank() || this.webhookToken == null || this.webhookToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                tokenRecebido.getBytes(StandardCharsets.UTF_8),
                this.webhookToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}

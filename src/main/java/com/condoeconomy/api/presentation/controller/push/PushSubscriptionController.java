package com.condoeconomy.api.presentation.controller.push;

import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPushSubscriptionRepository;
import com.condoeconomy.api.presentation.dto.push.PushSubscriptionRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
public class PushSubscriptionController {

    private static final Logger log = LoggerFactory.getLogger(PushSubscriptionController.class);

    private final SpringDataPushSubscriptionRepository repository;

    @PostMapping("/subscribe")
    @Transactional
    public ResponseEntity<Void> subscribe(Authentication auth, @RequestBody @Valid PushSubscriptionRequestDTO dto) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();

        SecuritySanitizer.validarTextoSeguro(dto.getEndpoint(), "endpoint");
        if (dto.getEndpoint() == null || !dto.getEndpoint().trim().toLowerCase().startsWith("https://")) {
            log.warn("[AUDITORIA DE SEGURANÇA] Tentativa de registro de Push Endpoint não-HTTPS (potencial SSRF) pelo usuário {}: {}",
                    user.getEmail(), dto.getEndpoint());
            throw new IllegalArgumentException("O endpoint de Web Push deve utilizar protocolo seguro (HTTPS).");
        }

        if (dto.getKeys() == null) {
            throw new IllegalArgumentException("As chaves de criptografia do Web Push são obrigatórias.");
        }
        SecuritySanitizer.validarTextoSeguro(dto.getKeys().getP256dh(), "p256dh");
        SecuritySanitizer.validarTextoSeguro(dto.getKeys().getAuth(), "auth");

        String endpointLimpo = dto.getEndpoint().trim();

        // Remove old if exists
        repository.deleteByEndpoint(endpointLimpo);

        PushSubscriptionJpaEntity sub = new PushSubscriptionJpaEntity();
        sub.setUsuarioId(user.getId());
        sub.setEndpoint(endpointLimpo);
        sub.setP256dh(dto.getKeys().getP256dh().trim());
        sub.setAuth(dto.getKeys().getAuth().trim());

        repository.save(sub);
        log.info("[AUDITORIA DE NOTIFICAÇÃO] Assinatura Web Push registrada com sucesso para o usuário ID={}", user.getId());
        return ResponseEntity.ok().build();
    }
}

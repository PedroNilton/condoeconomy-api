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

import java.net.URI;
import java.util.Locale;

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
        String endpointLimpo = validarEndpointSeguroContraSsrf(dto.getEndpoint(), user.getEmail());

        if (dto.getKeys() == null || dto.getKeys().getP256dh() == null || dto.getKeys().getP256dh().isBlank()
                || dto.getKeys().getAuth() == null || dto.getKeys().getAuth().isBlank()) {
            throw new IllegalArgumentException("As chaves de criptografia do Web Push são obrigatórias.");
        }
        SecuritySanitizer.validarTextoSeguro(dto.getKeys().getP256dh(), "p256dh");
        SecuritySanitizer.validarTextoSeguro(dto.getKeys().getAuth(), "auth");

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

    static String validarEndpointSeguroContraSsrf(String endpointBruto, String emailUsuario) {
        if (endpointBruto == null || endpointBruto.isBlank()) {
            throw new IllegalArgumentException("O endpoint de Web Push deve utilizar protocolo seguro (HTTPS).");
        }
        String endpointLimpo = endpointBruto.trim();
        URI uri;
        try {
            uri = URI.create(endpointLimpo);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("URL de endpoint Web Push malformada.");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            log.warn("[AUDITORIA DE SEGURANÇA] Tentativa de registro de Push Endpoint não-HTTPS (potencial SSRF) pelo usuário {}: {}",
                    emailUsuario, endpointLimpo);
            throw new IllegalArgumentException("O endpoint de Web Push deve utilizar protocolo seguro (HTTPS).");
        }
        if (uri.getUserInfo() != null) {
            log.warn("[AUDITORIA DE SEGURANÇA] Tentativa de registro de Push Endpoint com credenciais embutidas (SSRF) pelo usuário {}",
                    emailUsuario);
            throw new IllegalArgumentException("O endpoint de Web Push não pode conter credenciais na URL.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank() || isHostInternoOuPrivado(host)) {
            log.warn("[AUDITORIA DE SEGURANÇA] Tentativa de SSRF bloqueada no Web Push pelo usuário {}: host='{}'",
                    emailUsuario, host);
            throw new IllegalArgumentException("Destino de endpoint Web Push não permitido por motivos de segurança (proteção anti-SSRF).");
        }

        return endpointLimpo;
    }

    private static boolean isHostInternoOuPrivado(String rawHost) {
        String host = rawHost.toLowerCase(Locale.ROOT);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.equals("localhost")
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".internal")
                || host.equals("metadata.google.internal")) {
            return true;
        }
        // IPv6 loopback, indefinido, link-local (fe80::/10) e ULA privado (fc00::/7)
        if (host.contains(":")) {
            if (host.equals("::1") || host.equals("::") || host.startsWith("fe80:")
                    || host.startsWith("fc") || host.startsWith("fd") || host.startsWith("::ffff:")) {
                return true;
            }
        }
        // IPv4 literal: loopback (127.0.0.0/8), 0.0.0.0/8, RFC1918 (10/8, 172.16/12, 192.168/16), link-local/cloud metadata (169.254/16)
        if (host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            String[] parts = host.split("\\.");
            int o1 = Integer.parseInt(parts[0]);
            int o2 = Integer.parseInt(parts[1]);
            if (o1 == 0 || o1 == 10 || o1 == 127) {
                return true;
            }
            if (o1 == 169 && o2 == 254) {
                return true;
            }
            if (o1 == 172 && o2 >= 16 && o2 <= 31) {
                return true;
            }
            if (o1 == 192 && o2 == 168) {
                return true;
            }
            if (o1 == 100 && o2 >= 64 && o2 <= 127) {
                return true;
            }
        }
        return false;
    }
}

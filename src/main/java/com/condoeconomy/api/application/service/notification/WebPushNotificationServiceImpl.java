package com.condoeconomy.api.application.service.notification;

import com.condoeconomy.api.infrastructure.persistence.entity.PushSubscriptionJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPushSubscriptionRepository;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class WebPushNotificationServiceImpl implements PushNotificationService {

    @Value("${vapid.public.key}")
    private String publicKey;

    @Value("${vapid.private.key}")
    private String privateKey;

    private PushService pushService;
    private final SpringDataPushSubscriptionRepository subscriptionRepository;

    @PostConstruct
    public void init() throws GeneralSecurityException {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        pushService = new PushService();
        pushService.setPublicKey(publicKey);
        pushService.setPrivateKey(privateKey);
        pushService.setSubject("mailto:admin@condoeconomy.com");
    }

    @Override
    public void enviarPush(String tokenDispositivo, String titulo, String corpo) {
        // We will overload this or find the user subscriptions
        // Wait, tokenDispositivo was assumed to be FCM token.
        // Let's assume tokenDispositivo is now the "usuarioId" as String!
        try {
            UUID userId = UUID.fromString(tokenDispositivo);
            List<PushSubscriptionJpaEntity> subs = subscriptionRepository.findByUsuarioId(userId);
            
            for (PushSubscriptionJpaEntity sub : subs) {
                try {
                    Subscription subscription = new Subscription(sub.getEndpoint(), new Subscription.Keys(sub.getP256dh(), sub.getAuth()));
                    String payload = String.format("{\"title\":\"%s\",\"body\":\"%s\"}", titulo, corpo);
                    Notification notification = new Notification(subscription, payload);
                    pushService.send(notification);
                } catch (Exception e) {
                    System.err.println("Erro ao enviar para endpoint " + sub.getEndpoint() + ": " + e.getMessage());
                    // If gone, we should delete the subscription
                    if (e.getMessage().contains("410") || e.getMessage().contains("404")) {
                        subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("tokenDispositivo deve ser o usuarioId para WebPush");
        }
    }
}

package com.condoeconomy.api.application.service.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FirebasePushNotificationServiceImpl implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushNotificationServiceImpl.class);

    private final String credentialsPath;

    @Autowired
    public FirebasePushNotificationServiceImpl(
            @Value("${firebase.credentials.path:}") String credentialsPath) {
        this.credentialsPath = credentialsPath;
    }

    public FirebasePushNotificationServiceImpl() {
        this("");
    }

    @PostConstruct
    public void inicializarSeConfigurado() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.info("[FIREBASE PUSH] FIREBASE_CREDENTIALS_PATH não definido. Push notifications operarão em modo local seguro.");
            return;
        }
        Path caminho = Path.of(credentialsPath);
        if (!Files.exists(caminho)) {
            log.warn("[FIREBASE PUSH] Arquivo de credenciais não encontrado em '{}'. Mantendo modo local.", credentialsPath);
            return;
        }
        try (InputStream serviceAccount = Files.newInputStream(caminho)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("[FIREBASE PUSH] Firebase Admin SDK inicializado com sucesso.");
        } catch (Exception e) {
            log.error("[FIREBASE PUSH] Falha ao inicializar Firebase Admin SDK: {}", e.getMessage());
        }
    }

    @Override
    public void enviarPush(String tokenDispositivo, String titulo, String corpo) {
        if (tokenDispositivo == null || tokenDispositivo.isBlank()) {
            log.warn("[FIREBASE PUSH] Envio ignorado: tokenDispositivo vazio ou nulo.");
            return;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(titulo)
                    .setBody(corpo)
                    .build();

            Message message = Message.builder()
                    .setToken(tokenDispositivo)
                    .setNotification(notification)
                    .build();

            if (!FirebaseApp.getApps().isEmpty()) {
                String messageId = FirebaseMessaging.getInstance().send(message);
                log.info("[FIREBASE PUSH] Push enviado via FCM com sucesso (messageId={}).", messageId);
            } else {
                log.info("[FIREBASE PUSH - MODO LOCAL] Firebase não inicializado. Push registrado localmente para token='{}' | título='{}'.",
                        mascararToken(tokenDispositivo), titulo);
            }
        } catch (Exception e) {
            log.error("[FIREBASE PUSH] Erro ao enviar push notification: {}", e.getMessage());
        }
    }

    private String mascararToken(String token) {
        if (token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}

package com.condoeconomy.api.application.service.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class FirebasePushNotificationServiceImpl implements PushNotificationService {

    @Override
    public void enviarPush(String tokenDispositivo, String titulo, String corpo) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(titulo)
                    .setBody(corpo)
                    .build();

            Message message = Message.builder()
                    .setToken(tokenDispositivo)
                    .setNotification(notification)
                    .build();

            // TODO: Descomentar quando o Firebase Admin SDK for inicializado com a chave JSON real
            // String response = FirebaseMessaging.getInstance().send(message);
            
            System.out.println("Notificação Push simulada enviada para o token: " + tokenDispositivo);
            
        } catch (Exception e) {
            System.err.println("Erro ao enviar push notification: " + e.getMessage());
        }
    }
}

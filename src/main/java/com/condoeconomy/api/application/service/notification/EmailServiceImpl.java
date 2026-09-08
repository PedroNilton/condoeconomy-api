package com.condoeconomy.api.application.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void enviarEmail(String destinatario, String assunto, String corpoHtml) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpoHtml, true);
            
            // TODO: Descomentar a linha abaixo quando o SMTP estiver configurado no application.properties
            // javaMailSender.send(message);
            
            System.out.println("E-mail simulado enviado para: " + destinatario);
            
        } catch (MessagingException e) {
            System.err.println("Erro ao preparar e-mail: " + e.getMessage());
        }
    }
}

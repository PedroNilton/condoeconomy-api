package com.condoeconomy.api.application.service.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender javaMailSender;
    private final String mailUsername;

    public EmailServiceImpl(JavaMailSender javaMailSender,
                            @Value("${spring.mail.username:}") String mailUsername) {
        this.javaMailSender = javaMailSender;
        this.mailUsername = mailUsername;
    }

    public boolean isSmtpConfigurado() {
        return mailUsername != null
                && !mailUsername.isBlank()
                && !mailUsername.equalsIgnoreCase("seuemail@gmail.com");
    }

    @Override
    public void enviarEmail(String destinatario, String assunto, String corpoHtml) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (isSmtpConfigurado()) {
                helper.setFrom(mailUsername);
            }
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpoHtml, true);

            if (isSmtpConfigurado()) {
                javaMailSender.send(message);
                log.info("E-mail transacional enviado via SMTP para: {}", destinatario);
            } else {
                log.info("[MODO LOCAL - SMTP não configurado] E-mail preparado para '{}' | Assunto: '{}'", destinatario, assunto);
            }
        } catch (MessagingException | MailException e) {
            log.error("Falha ao enviar e-mail para '{}': {}", destinatario, e.getMessage());
        }
    }
}

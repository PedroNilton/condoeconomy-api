package com.condoeconomy.api.infrastructure.notification;

import com.condoeconomy.api.application.gateway.NotificationGateway;
import com.condoeconomy.api.domain.entity.notificacao.Notificacao;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.infrastructure.repository.notificacao.NotificacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NotificationGatewayImpl implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGatewayImpl.class);
    
    private final SpringDataUsuarioRepository usuarioRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationGatewayImpl(SpringDataUsuarioRepository usuarioRepository, 
                                   NotificacaoRepository notificacaoRepository, 
                                   SimpMessagingTemplate messagingTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notificarMoradoresDaUnidade(UUID unidadeId, String titulo, String mensagem) {
        log.info("[DISPARO DE PUSH NOTIFICATION]");
    }

    @Override
    public void notificarMorador(String destinatario, String unidadeTexto, String titulo, String mensagem) {
        log.info("Buscando morador para notificação: Destinatário={}, Unidade={}", destinatario, unidadeTexto);
        
        // Find user by matching partial name. If we find them, save notification and push websocket.
        List<UsuarioJpaEntity> usuarios = usuarioRepository.findAll();
        for (UsuarioJpaEntity u : usuarios) {
            if ("MORADOR".equals(u.getPapel()) && destinatario != null && u.getNome().toLowerCase().contains(destinatario.toLowerCase().trim())) {
                log.info("Morador encontrado! Salvando notificacao para {}", u.getNome());
                
                Notificacao notif = new Notificacao(UUID.randomUUID(), titulo, mensagem, u.getId());
                notificacaoRepository.save(notif);
                
                // Dispara Websocket para atualizar o ícone de sino e a lista
                messagingTemplate.convertAndSend("/topic/notificacoes/" + u.getId(), "UPDATE");
            }
        }
    }
}

package com.condoeconomy.api.core.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.condoeconomy.api.core.security.TokenService;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.presentation.controller.AuthController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);
    private static final String ATTR_SESSION_TOKEN = "WS_SESSION_TOKEN";

    private final TokenService tokenService;
    private final SpringDataUsuarioRepository usuarioRepository;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://app.condoeconomy.com}")
    private String allowedOrigins;

    public WebSocketConfig(TokenService tokenService, SpringDataUsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = Arrays.stream((allowedOrigins != null ? allowedOrigins : "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins.length > 0 ? origins : new String[]{"http://localhost:*"})
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            HttpServletRequest httpReq = servletRequest.getServletRequest();
                            Cookie[] cookies = httpReq.getCookies();
                            if (cookies != null) {
                                for (Cookie c : cookies) {
                                    if (AuthController.SESSION_COOKIE_NAME.equals(c.getName())
                                            && c.getValue() != null && !c.getValue().isBlank()) {
                                        attributes.put(ATTR_SESSION_TOKEN, c.getValue().trim());
                                        break;
                                    }
                                }
                            }
                        }
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                    }
                });
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = extrairTokenConnect(accessor);
                    if (token == null) {
                        log.warn("[AUDITORIA WEBSOCKET] Conexão STOMP rejeitada: JWT ausente no frame CONNECT.");
                        throw new MessageDeliveryException("Autenticação obrigatória para conexão WebSocket.");
                    }
                    DecodedJWT decoded = tokenService.verifyAndDecode(token);
                    if (decoded == null || decoded.getSubject() == null) {
                        log.warn("[AUDITORIA WEBSOCKET] Conexão STOMP rejeitada: JWT inválido ou expirado.");
                        throw new MessageDeliveryException("Token JWT inválido ou expirado.");
                    }
                    Integer tokenVersion = decoded.getClaim("tokenVersion").isNull()
                            ? null : decoded.getClaim("tokenVersion").asInt();

                    var usuarioOpt = usuarioRepository.findByEmail(decoded.getSubject());
                    if (usuarioOpt.isEmpty() || !usuarioOpt.get().isEnabled()) {
                        throw new MessageDeliveryException("Conta de usuário inexistente ou desativada.");
                    }
                    var usuario = usuarioOpt.get();
                    if (tokenVersion != null && tokenVersion != usuario.getTokenVersion()) {
                        log.warn("[AUDITORIA WEBSOCKET] Token revogado interceptado no WebSocket para '{}'", usuario.getEmail());
                        throw new MessageDeliveryException("Sessão revogada.");
                    }
                    accessor.setUser(new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    if (accessor.getUser() == null) {
                        throw new MessageDeliveryException("Assinatura de tópico negada: sessão não autenticada.");
                    }
                }
                return message;
            }

            private String extrairTokenConnect(StompHeaderAccessor accessor) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader == null) {
                    authHeader = accessor.getFirstNativeHeader("authorization");
                }
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String t = authHeader.substring(7).trim();
                    if (!t.isEmpty()) {
                        return t;
                    }
                }
                Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
                if (sessionAttrs != null && sessionAttrs.get(ATTR_SESSION_TOKEN) instanceof String cookieToken) {
                    return cookieToken;
                }
                return null;
            }
        });
    }
}

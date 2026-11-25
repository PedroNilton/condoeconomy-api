package com.condoeconomy.api.core.security;

import com.condoeconomy.api.application.usecase.financeiro.DarBaixaBoletoUseCase;
import com.condoeconomy.api.core.config.WebSocketConfig;
import com.condoeconomy.api.core.tenant.TenantContext;
import com.condoeconomy.api.core.tenant.TenantFilter;
import com.condoeconomy.api.core.tenant.TenantIdentifierResolver;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.presentation.controller.AuthController;
import com.condoeconomy.api.presentation.controller.webhook.WebhookAsaasController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SecurityValidationHardeningTest {

    private static final String TEST_SECRET = "test-secret-key-with-at-least-32-characters-long-256bits";

    private TokenService tokenService;
    private SpringDataUsuarioRepository usuarioRepository;
    private SecurityAuditService auditService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();

        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(tokenService, "activeProfile", "local");
        tokenService.validarSecret();

        usuarioRepository = mock(SpringDataUsuarioRepository.class);
        auditService = mock(SecurityAuditService.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    // ==================== TenantFilter (Multi-Tenant Isolation) ====================

    @Test
    @DisplayName("TenantFilter: normaliza X-Tenant-ID válido para minúsculas e limpa TenantContext após o chain")
    void deveNormalizarTenantValidoELimparContextoAposRequisicao() throws Exception {
        TenantFilter filter = new TenantFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "  Condo_Solar_1  ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> tenantDuranteRequisicao = new AtomicReference<>();
        FilterChain chain = (req, res) -> tenantDuranteRequisicao.set(TenantContext.getCurrentTenant());

        filter.doFilter(request, response, chain);

        assertEquals("condo_solar_1", tenantDuranteRequisicao.get());
        assertNull(TenantContext.getCurrentTenant(), "TenantContext deve ser limpo no bloco finally após a requisição");
    }

    @Test
    @DisplayName("TenantFilter: recua para schema padrão 'public' quando X-Tenant-ID é malicioso, vazio ou acima de 63 chars")
    void deveRecuarParaTenantPadraoQuandoHeaderForInvalidoOuMalicioso() throws Exception {
        TenantFilter filter = new TenantFilter();
        List<String> entradasInvalidas = List.of(
                "public; DROP TABLE usuarios;--",
                "../etc/passwd",
                "condo com espaco",
                "condo-hifen",
                "a".repeat(64),
                "   "
        );

        for (String headerInvalido : entradasInvalidas) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-ID", headerInvalido);
            AtomicReference<String> tenantResolvido = new AtomicReference<>();

            filter.doFilter(request, new MockHttpServletResponse(),
                    (req, res) -> tenantResolvido.set(TenantContext.getCurrentTenant()));

            assertEquals(TenantIdentifierResolver.DEFAULT_TENANT, tenantResolvido.get(),
                    "Entrada inválida '" + headerInvalido + "' deve recuar para o schema padrão");
            assertNull(TenantContext.getCurrentTenant());
        }
    }

    @Test
    @DisplayName("TenantFilter: limpa TenantContext no finally mesmo quando ocorre exceção downstream")
    void deveLimparTenantContextMesmoQuandoFilterChainLancarExcecao() {
        TenantFilter filter = new TenantFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "condominio_mar_egeu");

        FilterChain chainComErro = (req, res) -> {
            assertEquals("condominio_mar_egeu", TenantContext.getCurrentTenant());
            throw new IllegalStateException("Falha simulada no controller");
        };

        assertThrows(IllegalStateException.class,
                () -> filter.doFilter(request, new MockHttpServletResponse(), chainComErro));
        assertNull(TenantContext.getCurrentTenant(), "ThreadLocal não pode vazar tenant entre requisições após exceção");
    }

    // ==================== WebhookAsaasController (Constant-Time & Idempotency) ====================

    @Test
    @DisplayName("Webhook Asaas: rejeita com HTTP 401 quando asaas-access-token está ausente, vazio ou incorreto")
    void deveRejeitarWebhookAsaasComTokenAusenteOuInvalido() {
        DarBaixaBoletoUseCase darBaixaUseCase = mock(DarBaixaBoletoUseCase.class);
        WebhookAsaasController controller = new WebhookAsaasController(darBaixaUseCase, "segredo-asaas-super-forte");

        Map<String, Object> payload = Map.of(
                "event", "PAYMENT_RECEIVED",
                "payment", Map.of("id", "pay_001", "externalReference", UUID.randomUUID().toString())
        );

        assertEquals(HttpStatus.UNAUTHORIZED, controller.receberWebhookPagamento(null, payload).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.receberWebhookPagamento("   ", payload).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.receberWebhookPagamento("segredo-errado", payload).getStatusCode());
        verify(darBaixaUseCase, never()).executar(any());
    }

    @Test
    @DisplayName("Webhook Asaas: processa PAYMENT_RECEIVED válido e bloqueia replay duplicado com idempotência")
    void deveProcessarWebhookPagamentoValidoEGarantirIdempotenciaContraReplay() {
        DarBaixaBoletoUseCase darBaixaUseCase = mock(DarBaixaBoletoUseCase.class);
        WebhookAsaasController controller = new WebhookAsaasController(darBaixaUseCase, "segredo-asaas-super-forte");

        UUID boletoId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "event", "PAYMENT_RECEIVED",
                "payment", Map.of("id", "pay_idempotente_99", "externalReference", boletoId.toString())
        );

        var primeiraResposta = controller.receberWebhookPagamento("segredo-asaas-super-forte", payload);
        var segundaRespostaReplay = controller.receberWebhookPagamento("segredo-asaas-super-forte", payload);

        assertEquals(HttpStatus.OK, primeiraResposta.getStatusCode());
        assertEquals(HttpStatus.OK, segundaRespostaReplay.getStatusCode());
        verify(darBaixaUseCase, times(1)).executar(boletoId);
    }

    @Test
    @DisplayName("Webhook Asaas: ignora eventos diferentes de PAYMENT_RECEIVED sem acionar baixa de boleto")
    void deveIgnorarEventosNaoPagamentoNoWebhookAsaasComTokenValido() {
        DarBaixaBoletoUseCase darBaixaUseCase = mock(DarBaixaBoletoUseCase.class);
        WebhookAsaasController controller = new WebhookAsaasController(darBaixaUseCase, "segredo-asaas-super-forte");

        Map<String, Object> payload = Map.of(
                "event", "PAYMENT_CREATED",
                "payment", Map.of("id", "pay_002", "externalReference", UUID.randomUUID().toString())
        );

        var response = controller.receberWebhookPagamento("segredo-asaas-super-forte", payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(darBaixaUseCase, never()).executar(any());
    }

    // ==================== WebSocketConfig (STOMP JWT ChannelInterceptor) ====================

    private ChannelInterceptor extrairStompInterceptor() {
        WebSocketConfig config = new WebSocketConfig(tokenService, usuarioRepository);
        ChannelRegistration registration = new ChannelRegistration();
        config.configureClientInboundChannel(registration);
        @SuppressWarnings("unchecked")
        List<ChannelInterceptor> interceptors = (List<ChannelInterceptor>) ReflectionTestUtils.invokeMethod(registration, "getInterceptors");
        assertNotNull(interceptors);
        assertFalse(interceptors.isEmpty());
        return interceptors.get(0);
    }

    @Test
    @DisplayName("WebSocket STOMP: rejeita frame CONNECT sem token JWT")
    void deveRejeitarConexaoWebSocketStompSemTokenJwt() {
        ChannelInterceptor interceptor = extrairStompInterceptor();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class)));
    }

    @Test
    @DisplayName("WebSocket STOMP: rejeita frame CONNECT quando tokenVersion foi revogado ou conta está desativada")
    void deveRejeitarConexaoWebSocketStompComTokenRevogadoOuContaDesativada() {
        ChannelInterceptor interceptor = extrairStompInterceptor();

        UsuarioJpaEntity usuario = new UsuarioJpaEntity();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("morador.ws@habitos.com");
        usuario.setPapel("ROLE_MORADOR");
        usuario.setTokenVersion(1);
        usuario.setAtivo(true);

        String jwtAntigo = tokenService.generateToken(usuario);

        // Cenário 1: token revogado (tokenVersion incrementado)
        usuario.setTokenVersion(2);
        when(usuarioRepository.findByEmail("morador.ws@habitos.com")).thenReturn(Optional.of(usuario));

        StompHeaderAccessor accessorRevogado = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessorRevogado.setNativeHeader("Authorization", "Bearer " + jwtAntigo);
        accessorRevogado.setLeaveMutable(true);
        Message<byte[]> msgRevogada = MessageBuilder.createMessage(new byte[0], accessorRevogado.getMessageHeaders());

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(msgRevogada, mock(MessageChannel.class)));

        // Cenário 2: conta desativada mesmo com tokenVersion compatível
        usuario.setTokenVersion(1);
        usuario.setAtivo(false);

        StompHeaderAccessor accessorInativo = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessorInativo.setNativeHeader("Authorization", "Bearer " + jwtAntigo);
        accessorInativo.setLeaveMutable(true);
        Message<byte[]> msgInativa = MessageBuilder.createMessage(new byte[0], accessorInativo.getMessageHeaders());

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(msgInativa, mock(MessageChannel.class)));
    }

    @Test
    @DisplayName("WebSocket STOMP: autentica CONNECT com Bearer JWT válido e bloqueia SUBSCRIBE sem usuário")
    void deveAutenticarConexaoWebSocketStompComBearerValidoEBloquearSubscribeSemSessao() {
        ChannelInterceptor interceptor = extrairStompInterceptor();

        UsuarioJpaEntity usuario = new UsuarioJpaEntity();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("porteiro.ws@habitos.com");
        usuario.setPapel("ROLE_PORTEIRO");
        usuario.setTokenVersion(4);
        usuario.setAtivo(true);

        String jwtValido = tokenService.generateToken(usuario);
        when(usuarioRepository.findByEmail("porteiro.ws@habitos.com")).thenReturn(Optional.of(usuario));

        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectAccessor.setNativeHeader("Authorization", "Bearer " + jwtValido);
        connectAccessor.setLeaveMutable(true);
        Message<byte[]> connectMsg = MessageBuilder.createMessage(new byte[0], connectAccessor.getMessageHeaders());

        Message<?> resultadoConnect = interceptor.preSend(connectMsg, mock(MessageChannel.class));
        assertNotNull(resultadoConnect);
        assertNotNull(connectAccessor.getUser(), "CONNECT autenticado deve associar principal à sessão STOMP");

        // SUBSCRIBE sem usuário autenticado deve ser bloqueado
        StompHeaderAccessor subAnonimo = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subAnonimo.setDestination("/topic/visitantes");
        subAnonimo.setLeaveMutable(true);
        Message<byte[]> subMsg = MessageBuilder.createMessage(new byte[0], subAnonimo.getMessageHeaders());

        assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(subMsg, mock(MessageChannel.class)));
    }

    @Test
    @DisplayName("WebSocket STOMP: autentica CONNECT via token capturado do Cookie HttpOnly no handshake")
    void deveAutenticarConexaoWebSocketStompViaCookieDeSessaoNoHandshake() {
        ChannelInterceptor interceptor = extrairStompInterceptor();

        UsuarioJpaEntity usuario = new UsuarioJpaEntity();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("sindico.ws@habitos.com");
        usuario.setPapel("ROLE_SINDICO");
        usuario.setTokenVersion(1);
        usuario.setAtivo(true);

        String jwtCookie = tokenService.generateToken(usuario);
        when(usuarioRepository.findByEmail("sindico.ws@habitos.com")).thenReturn(Optional.of(usuario));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("WS_SESSION_TOKEN", jwtCookie);
        accessor.setSessionAttributes(sessionAttrs);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertDoesNotThrow(() -> interceptor.preSend(message, mock(MessageChannel.class)));
        assertNotNull(accessor.getUser());
    }

    // ==================== SecuritySanitizer (Edge Cases & DoS Limits) ====================

    @Test
    @DisplayName("SecuritySanitizer: rejeita imagem Base64 acima do limite máximo de 2 MB contra DoS")
    void deveBloquearImagemBase64AcimaDoLimiteMaximoDe2Mb() {
        String payloadExcedente = "data:image/png;base64," + "A".repeat(2_800_001);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SecuritySanitizer.validarUrlFotoSegura(payloadExcedente));
        assertTrue(ex.getMessage().contains("2 MB"));
    }

    @Test
    @DisplayName("SecuritySanitizer: rejeita data:text/html, vbscript:, file:// e URLs HTTP com vetores XSS")
    void deveRejeitarProtocolosInsegurosEUrlsComPadroesMaliciososEmFotos() {
        assertDoesNotThrow(() -> SecuritySanitizer.validarUrlFotoSegura(null));
        assertDoesNotThrow(() -> SecuritySanitizer.validarUrlFotoSegura("   "));
        assertDoesNotThrow(() -> SecuritySanitizer.validarUrlFotoSegura("data:image/jpeg;base64,/9j/4AAQ"));
        assertDoesNotThrow(() -> SecuritySanitizer.validarUrlFotoSegura("data:image/webp;base64,UklGRh4AAABXRUJQ"));
        assertDoesNotThrow(() -> SecuritySanitizer.validarUrlFotoSegura("http://cdn.local/foto.jpg"));

        assertThrows(IllegalArgumentException.class,
                () -> SecuritySanitizer.validarUrlFotoSegura("data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg=="));
        assertThrows(IllegalArgumentException.class,
                () -> SecuritySanitizer.validarUrlFotoSegura("vbscript:msgbox(1)"));
        assertThrows(IllegalArgumentException.class,
                () -> SecuritySanitizer.validarUrlFotoSegura("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class,
                () -> SecuritySanitizer.validarUrlFotoSegura("https://cdn.habitos.com/a.png?q=<script>alert(1)</script>"));
    }

    @Test
    @DisplayName("SecuritySanitizer: remove tags HTML e caracteres de controle e trata casos limite de mascaramento LGPD")
    void deveSanitizarHtmlECaracteresDeControleETratarCasosLimiteDeMascaramento() {
        assertNull(SecuritySanitizer.sanitizar(null));
        assertEquals("Aviso importante", SecuritySanitizer.sanitizar("  <b>Aviso</b>\u0000 importante\u0007  "));

        assertNull(SecuritySanitizer.mascararDocumento(null));
        assertNull(SecuritySanitizer.mascararDocumento("   "));
        assertEquals("****", SecuritySanitizer.mascararDocumento("1234"));

        assertEquals("***@***.***", SecuritySanitizer.mascararEmail(null));
        assertEquals("***@***.***", SecuritySanitizer.mascararEmail("   "));
        assertEquals("*@*@b.com", SecuritySanitizer.mascararEmail("a@b.com"));
        assertEquals("*@*", SecuritySanitizer.mascararEmail("sem-arroba"));

        assertNull(SecuritySanitizer.sanitizarCsvFormula(null));
        assertEquals("   ", SecuritySanitizer.sanitizarCsvFormula("   "));
    }

    // ==================== TokenService & SecurityFilter Extra Hardening ====================

    @Test
    @DisplayName("TokenService: exige chave mínima de 32 bytes e rejeita token adulterado ou assinado com outra chave")
    void deveRejeitarJwtSecretMenorQue32BytesETokenAdulterado() {
        TokenService secretCurto = new TokenService();
        ReflectionTestUtils.setField(secretCurto, "secret", "chave-curta-insegura");
        ReflectionTestUtils.setField(secretCurto, "activeProfile", "local");
        assertThrows(IllegalStateException.class, secretCurto::validarSecret);

        UsuarioJpaEntity usuario = new UsuarioJpaEntity();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("ana@habitos.com");
        usuario.setPapel("ROLE_SINDICO");
        usuario.setTokenVersion(1);

        String jwtValido = tokenService.generateToken(usuario);
        assertEquals("ana@habitos.com", tokenService.validateToken(jwtValido));
        assertNull(tokenService.verifyAndDecode(jwtValido + "adulterado"));
        assertNull(tokenService.validateToken("token.malformado"));

        TokenService outroServico = new TokenService();
        ReflectionTestUtils.setField(outroServico, "secret", "outra-chave-completamente-diferente-com-32-bytes-minimo");
        ReflectionTestUtils.setField(outroServico, "activeProfile", "local");
        outroServico.validarSecret();

        assertNull(outroServico.verifyAndDecode(jwtValido),
                "JWT assinado com outra chave secreta deve ser rejeitado");
    }

    @Test
    @DisplayName("SecurityFilter: bloqueia autenticação para usuário desativado mesmo com JWT válido")
    void deveBloquearAutenticacaoNoSecurityFilterParaContaDesativada() throws Exception {
        SecurityFilter securityFilter = new SecurityFilter(tokenService, usuarioRepository, auditService);

        UsuarioJpaEntity usuarioInativo = new UsuarioJpaEntity();
        usuarioInativo.setId(UUID.randomUUID());
        usuarioInativo.setEmail("bloqueado@habitos.com");
        usuarioInativo.setPapel("ROLE_MORADOR");
        usuarioInativo.setTokenVersion(1);
        usuarioInativo.setAtivo(false);

        String jwt = tokenService.generateToken(usuarioInativo);
        when(usuarioRepository.findByEmail("bloqueado@habitos.com")).thenReturn(Optional.of(usuarioInativo));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        securityFilter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Usuário com ativo=false não deve ser autenticado no SecurityContext");
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("SecurityFilter: permite acesso multi-tenant com X-Tenant-ID distinto para ROLE_SUPER_ADMIN")
    void devePermitirTrocaDeTenantNoSecurityFilterApenasParaSuperAdmin() throws Exception {
        SecurityFilter securityFilter = new SecurityFilter(tokenService, usuarioRepository, auditService);
        TenantContext.setCurrentTenant("public");

        UsuarioJpaEntity superAdmin = new UsuarioJpaEntity();
        superAdmin.setId(UUID.randomUUID());
        superAdmin.setEmail("superadmin@habitos.com");
        superAdmin.setPapel("ROLE_SUPER_ADMIN");
        superAdmin.setTokenVersion(1);
        superAdmin.setAtivo(true);

        String jwt = tokenService.generateToken(superAdmin);
        when(usuarioRepository.findByEmail("superadmin@habitos.com")).thenReturn(Optional.of(superAdmin));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        request.addHeader("X-Tenant-ID", "condominio_beta");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        securityFilter.doFilterInternal(request, response, chain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(auditService, never()).registrarUsuario(any(), eq("TENTATIVA_SPOOFING_TENANT"), any(), any(), any());
        verify(chain, times(1)).doFilter(request, response);
    }

    // ==================== AuthController (Logout Kill Switch & Anti-Injection/Flood) ====================

    @Test
    @DisplayName("AuthController: logout incrementa tokenVersion no servidor e expira cookie HttpOnly HABITOS_SESSION")
    void deveRevogarTokenVersionELimparCookieHttpOnlyNoLogout() {
        AuthenticationManager authManager = mock(AuthenticationManager.class);
        LoginAttemptService loginAttemptService = new LoginAttemptService();
        PasswordRecoveryService recoveryService = mock(PasswordRecoveryService.class);

        AuthController authController = new AuthController(
                authManager, tokenService, loginAttemptService, auditService, recoveryService, usuarioRepository
        );

        UsuarioJpaEntity usuario = new UsuarioJpaEntity();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("titular.logout@habitos.com");
        usuario.setPapel("ROLE_MORADOR");
        usuario.setTokenVersion(5);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(true);
        request.setRemoteAddr("10.0.0.15");

        var response = authController.logout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(6, usuario.getTokenVersion(), "Logout deve incrementar tokenVersion para revogar JWTs ativos");
        verify(usuarioRepository).save(usuario);
        verify(auditService).registrarUsuario(eq(usuario), eq("LOGOUT_SUCESSO"), eq("INFO"), eq("10.0.0.15"), anyString());

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains(AuthController.SESSION_COOKIE_NAME + "="));
        assertTrue(setCookie.contains("Max-Age=0"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertTrue(setCookie.contains("Secure"));
    }

    @Test
    @DisplayName("AuthController: bloqueia injeção e força bruta no login e aplica rate limit na recuperação de senha")
    void deveBloquearPayloadInjecaoEForcaBrutaNoLoginEFloodNaRecuperacao() {
        AuthenticationManager authManager = mock(AuthenticationManager.class);
        LoginAttemptService loginAttemptService = new LoginAttemptService();
        PasswordRecoveryService recoveryService = mock(PasswordRecoveryService.class);

        AuthController authController = new AuthController(
                authManager, tokenService, loginAttemptService, auditService, recoveryService, usuarioRepository
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.42");

        // 1. Tentativa de SQLi no login
        var respInjecao = authController.login(
                new AuthController.LoginRequestDTO("admin' OR 1=1 --", "123456"), request);
        assertEquals(HttpStatus.BAD_REQUEST, respInjecao.getStatusCode());
        verify(authManager, never()).authenticate(any());
        verify(auditService).registrar(isNull(), eq("admin' OR 1=1 --"), eq("N/A"),
                eq("TENTATIVA_INJECAO_LOGIN"), eq("CRITICO"), eq("198.51.100.42"), anyString());

        // 2. Bloqueio de força bruta no login (429 Too Many Requests)
        String emailAlvo = "vitima@habitos.com";
        for (int i = 0; i < 5; i++) {
            loginAttemptService.registrarFalha(emailAlvo, "198.51.100.42");
        }
        var respBloqueado = authController.login(
                new AuthController.LoginRequestDTO(emailAlvo, "SenhaQualquer123"), request);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, respBloqueado.getStatusCode());
        verify(authManager, never()).authenticate(any());

        // 3. Rate limit na recuperação de senha (máximo 5 solicitações por IP em 5 min)
        var dtoRecuperacao = new AuthController.RecuperarSenhaRequestDTO("usuario@habitos.com");
        for (int i = 0; i < 5; i++) {
            assertEquals(HttpStatus.OK, authController.recuperarSenha(dtoRecuperacao, request).getStatusCode());
        }
        var respFlood = authController.recuperarSenha(dtoRecuperacao, request);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, respFlood.getStatusCode());
        verify(recoveryService, times(5)).solicitarRecuperacao("usuario@habitos.com", "198.51.100.42");
    }
}

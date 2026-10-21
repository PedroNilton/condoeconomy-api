package com.condoeconomy.api.core.security;

import com.condoeconomy.api.core.tenant.TenantContext;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.presentation.controller.AuthController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SecurityDefenseInDepthTest {

    private TokenService tokenService;
    private SpringDataUsuarioRepository usuarioRepository;
    private SecurityAuditService auditService;
    private SecurityFilter securityFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();

        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret-key-with-at-least-32-characters-long-256bits");
        tokenService.validarSecret();

        usuarioRepository = Mockito.mock(SpringDataUsuarioRepository.class);
        auditService = Mockito.mock(SecurityAuditService.class);
        securityFilter = new SecurityFilter(tokenService, usuarioRepository, auditService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Camada 2: Deve bloquear tentativas de SQL Injection no SecuritySanitizer")
    void deveBloquearTentativasDeSqlInjection() {
        assertThrows(IllegalArgumentException.class, () ->
                SecuritySanitizer.validarTextoSeguro("admin' OR 1=1 --", "email"));
        assertThrows(IllegalArgumentException.class, () ->
                SecuritySanitizer.validarTextoSeguro("teste; DROP TABLE usuario", "nome"));
        assertThrows(IllegalArgumentException.class, () ->
                SecuritySanitizer.validarTextoSeguro("1 UNION SELECT senha FROM usuario", "filtro"));
    }

    @Test
    @DisplayName("Camada 2: Deve bloquear vetores XSS e caracteres de controle/Null Byte")
    void deveBloquearTentativasDeXssECaracteresDeControle() {
        assertThrows(IllegalArgumentException.class, () ->
                SecuritySanitizer.validarTextoSeguro("<script>alert('xss')</script>", "descricao"));
        assertThrows(IllegalArgumentException.class, () ->
                SecuritySanitizer.validarTextoSeguro("javascript:alert(document.cookie)", "link"));
        assertThrows(IllegalArgumentException.class, () ->
                SecuritySanitizer.validarTextoSeguro("texto\u0000comNullByte", "nome"));
    }

    @Test
    @DisplayName("Camada 2: Deve validar URL de foto segura e bloquear SVG/JavaScript malicioso")
    void devePermitirUrlDeFotoSeguraEBloquearProtocolosMaliciosos() {
        assertDoesNotThrow(() -> SecuritySanitizer.validarUrlFotoSegura("https://cdn.habitos.com.br/avatar.png"));
        assertDoesNotThrow(() -> SecuritySanitizer.validarUrlFotoSegura("data:image/png;base64,iVBORw0KGgoAAAANSUhEUg=="));

        assertThrows(IllegalArgumentException.class, () ->
                SecuritySanitizer.validarUrlFotoSegura("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () ->
                SecuritySanitizer.validarUrlFotoSegura("data:image/svg+xml;base64,PHN2ZyBvbmxvYWQ9ImFsZXJ0KDEpIj4="));
    }

    @Test
    @DisplayName("Camada 2 (LGPD): Deve mascarar CPF e RG preservando minimização de dados")
    void deveMascararCpfERgConformeMinimizacaoLgpd() {
        assertEquals("***.456.789-**", SecuritySanitizer.mascararDocumento("123.456.789-01"));
        assertEquals("***.456.789-**", SecuritySanitizer.mascararDocumento("12345678901"));
        assertEquals("***.56**", SecuritySanitizer.mascararDocumento("12345678"));
    }

    @Test
    @DisplayName("Camada 1: Deve bloquear login após 5 falhas consecutivas e liberar após sucesso")
    void deveBloquearLoginAposCincoTentativasFalhasEResetarNoSucesso() {
        LoginAttemptService service = new LoginAttemptService();
        String email = "morador@habitos.com";
        String ip = "192.168.1.50";

        for (int i = 0; i < 4; i++) {
            service.registrarFalha(email, ip);
            assertFalse(service.estaBloqueado(email, ip));
        }

        service.registrarFalha(email, ip);
        assertTrue(service.estaBloqueado(email, ip), "Quinta falha consecutiva deve ativar o bloqueio temporário");

        service.registrarSucesso(email, ip);
        assertFalse(service.estaBloqueado(email, ip), "Login com sucesso deve limpar o contador de falhas");
    }

    @Test
    @DisplayName("Camada 1: Deve aplicar rate limit por IP em endpoints públicos")
    void deveLimitarTaxaDeRequisicoesEmEndpointsPublicos() {
        LoginAttemptService service = new LoginAttemptService();
        String ip = "203.0.113.10";

        assertTrue(service.verificarEContabilizarRequisicaoPublica(ip, "QR_CHECKIN", 2));
        assertTrue(service.verificarEContabilizarRequisicaoPublica(ip, "QR_CHECKIN", 2));
        assertFalse(service.verificarEContabilizarRequisicaoPublica(ip, "QR_CHECKIN", 2),
                "Requisição acima do limite por janela deve ser bloqueada");
    }

    @Test
    @DisplayName("Camadas 3 e 5: Deve autenticar via Cookie HttpOnly e confinar o Tenant no contexto")
    void deveAutenticarViaCookieHttpOnlyEConfinarTenantNoContexto() throws Exception {
        TenantContext.setCurrentTenant("condominio_horizonte");

        UsuarioJpaEntity usuario = new UsuarioJpaEntity();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("morador@horizonte.com");
        usuario.setPapel("ROLE_MORADOR");
        usuario.setTokenVersion(3);

        String jwt = tokenService.generateToken(usuario);
        when(usuarioRepository.findByEmail("morador@horizonte.com")).thenReturn(Optional.of(usuario));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthController.SESSION_COOKIE_NAME, jwt));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(),
                "Deve autenticar a sessão lendo o Cookie HttpOnly quando o header Authorization não for enviado");
        assertEquals("condominio_horizonte", TenantContext.getCurrentTenant());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Camada 3 (Tenant Sandbox): Deve bloquear X-Tenant-ID Spoofing com HTTP 403 e registrar auditoria crítica")
    void deveBloquearTentativaDeSpoofingDeTenantComHttp403ERegistrarAuditoria() throws Exception {
        TenantContext.setCurrentTenant("condominio_alfa");

        UsuarioJpaEntity usuario = new UsuarioJpaEntity();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("invasor@alfa.com");
        usuario.setPapel("ROLE_MORADOR");
        usuario.setTokenVersion(1);

        String jwt = tokenService.generateToken(usuario);
        when(usuarioRepository.findByEmail("invasor@alfa.com")).thenReturn(Optional.of(usuario));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwt);
        request.addHeader("X-Tenant-ID", "condominio_beta");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus(),
                "Tentativa de acessar outro condomínio via X-Tenant-ID deve retornar HTTP 403 Forbidden");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(auditService, times(1)).registrarUsuario(
                eq(usuario),
                eq("TENTATIVA_SPOOFING_TENANT"),
                eq("CRITICO"),
                anyString(),
                anyString()
        );
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Camada 5 (Token Kill Switch): Deve rejeitar token cuja versão foi revogada")
    void deveBloquearTokenRevogadoPeloKillSwitchQuandoTokenVersionDiverge() throws Exception {
        TenantContext.setCurrentTenant("public");

        UsuarioJpaEntity usuario = new UsuarioJpaEntity();
        usuario.setId(UUID.randomUUID());
        usuario.setEmail("titular@habitos.com");
        usuario.setPapel("ROLE_MORADOR");
        usuario.setTokenVersion(1);

        // Gera o token na versão 1
        String tokenAntigo = tokenService.generateToken(usuario);

        // Simula acionamento do Kill Switch (troca de senha ou encerramento de sessões) -> versão sobe para 2
        usuario.setTokenVersion(2);
        when(usuarioRepository.findByEmail("titular@habitos.com")).thenReturn(Optional.of(usuario));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenAntigo);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        securityFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "Token com tokenVersion anterior ao atual não deve autenticar o usuário");
    }
}

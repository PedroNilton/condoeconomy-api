package com.condoeconomy.api.core.security;

import com.condoeconomy.api.application.gateway.PedidoRepository;
import com.condoeconomy.api.application.gateway.ProdutoRepository;
import com.condoeconomy.api.application.service.notification.FirebasePushNotificationServiceImpl;
import com.condoeconomy.api.application.service.payment.AsaasGatewayPagamentoServiceImpl;
import com.condoeconomy.api.domain.entity.Produto;
import com.condoeconomy.api.infrastructure.persistence.entity.PreferenciaNotificacaoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.financeiro.BoletoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataMoradorAdicionalRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPetRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPreferenciaNotificacaoRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataVeiculoRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataBoletoRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataDespesaRepository;
import com.condoeconomy.api.presentation.controller.FinancasController;
import com.condoeconomy.api.presentation.controller.admin.AdminCondoStoreController;
import com.condoeconomy.api.presentation.controller.perfil.PerfilController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FluxosOperacionaisIntegracaoTest {

    private ProdutoRepository produtoRepository;
    private PedidoRepository pedidoRepository;
    private SpringDataUsuarioRepository usuarioRepository;
    private SpringDataBoletoRepository boletoRepository;
    private SpringDataDespesaRepository despesaRepository;
    private SpringDataPreferenciaNotificacaoRepository preferenciaRepository;
    private SecurityAuditService auditService;

    private AdminCondoStoreController adminCondoStoreController;
    private FinancasController financasController;
    private PerfilController perfilController;
    private AsaasGatewayPagamentoServiceImpl asaasServiceLocal;

    @BeforeEach
    void setUp() {
        produtoRepository = mock(ProdutoRepository.class);
        pedidoRepository = mock(PedidoRepository.class);
        usuarioRepository = mock(SpringDataUsuarioRepository.class);
        boletoRepository = mock(SpringDataBoletoRepository.class);
        despesaRepository = mock(SpringDataDespesaRepository.class);
        preferenciaRepository = mock(SpringDataPreferenciaNotificacaoRepository.class);
        auditService = mock(SecurityAuditService.class);

        asaasServiceLocal = new AsaasGatewayPagamentoServiceImpl("", "https://sandbox.asaas.com/api/v3", new ObjectMapper());

        adminCondoStoreController = new AdminCondoStoreController(
                produtoRepository,
                pedidoRepository,
                usuarioRepository,
                auditService
        );

        financasController = new FinancasController(
                boletoRepository,
                despesaRepository,
                asaasServiceLocal,
                auditService
        );

        perfilController = new PerfilController(
                mock(SpringDataVeiculoRepository.class),
                mock(SpringDataPetRepository.class),
                mock(SpringDataMoradorAdicionalRepository.class),
                preferenciaRepository,
                usuarioRepository,
                new BCryptPasswordEncoder(),
                mock(TokenService.class),
                auditService
        );
    }

    @Test
    @DisplayName("CondoStore Admin: cadastra novo produto e atualiza estoque com registro em trilha de auditoria")
    void deveCadastrarEAtualizarEstoqueProdutoComAuditoria() {
        UsuarioJpaEntity sindico = new UsuarioJpaEntity();
        sindico.setId(UUID.randomUUID());
        sindico.setEmail("sindico@condominio.com");
        sindico.setPapel("ROLE_SINDICO");

        when(produtoRepository.salvar(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        var criarDto = new AdminCondoStoreController.SalvarProdutoRequestDTO(
                "Água Mineral 500ml",
                "Sem gás, gelada",
                new BigDecimal("4.50"),
                24
        );

        var respCriacao = adminCondoStoreController.cadastrarProduto(criarDto, sindico, new MockHttpServletRequest());
        assertEquals(HttpStatus.CREATED, respCriacao.getStatusCode());

        AdminCondoStoreController.ProdutoAdminDTO criado = (AdminCondoStoreController.ProdutoAdminDTO) respCriacao.getBody();
        assertNotNull(criado);
        assertEquals(24, criado.quantidadeEstoque());

        when(produtoRepository.buscarPorId(criado.id())).thenReturn(Optional.of(
                new Produto(criado.id(), criado.nome(), criado.descricao(), criado.preco(), criado.quantidadeEstoque())
        ));

        var reporDto = new AdminCondoStoreController.SalvarProdutoRequestDTO(
                "Água Mineral 500ml",
                "Sem gás, gelada",
                new BigDecimal("4.50"),
                48
        );

        var respAtualizacao = adminCondoStoreController.atualizarProduto(criado.id(), reporDto, sindico, new MockHttpServletRequest());
        assertEquals(HttpStatus.OK, respAtualizacao.getStatusCode());

        AdminCondoStoreController.ProdutoAdminDTO atualizado = (AdminCondoStoreController.ProdutoAdminDTO) respAtualizacao.getBody();
        assertNotNull(atualizado);
        assertEquals(48, atualizado.quantidadeEstoque());

        verify(auditService).registrarUsuario(eq(sindico), eq("PRODUTO_CONDOSTORE_CRIADO"), eq("INFO"), any(), any());
        verify(auditService).registrarUsuario(eq(sindico), eq("ESTOQUE_CONDOSTORE_ATUALIZADO"), eq("INFO"), any(), any());
    }

    @Test
    @DisplayName("CondoStore Admin: bloqueia payload XSS ao cadastrar produto")
    void deveBloquearPayloadMaliciosoNoCondoStoreAdmin() {
        UsuarioJpaEntity sindico = new UsuarioJpaEntity();
        sindico.setId(UUID.randomUUID());
        sindico.setEmail("sindico@condominio.com");

        var dtoMalicioso = new AdminCondoStoreController.SalvarProdutoRequestDTO(
                "<script>alert('xss')</script>",
                "Descrição normal",
                new BigDecimal("10.00"),
                5
        );

        var resposta = adminCondoStoreController.cadastrarProduto(dtoMalicioso, sindico, new MockHttpServletRequest());
        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        verify(produtoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("Finanças Admin: emite boleto com linha digitável de contingência e registra auditoria")
    void deveEmitirBoletoGerandoLinhaDigitavelEAuditoria() {
        UsuarioJpaEntity sindico = new UsuarioJpaEntity();
        sindico.setId(UUID.randomUUID());
        sindico.setEmail("sindico@condominio.com");

        when(boletoRepository.save(any(BoletoJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = new FinancasController.EmitirBoletoRequestDTO(
                "Apto 101 - Bloco A",
                "Carlos Silva",
                "11/2026",
                new BigDecimal("685.50"),
                LocalDate.now().plusDays(10)
        );

        var resposta = financasController.emitirBoleto(dto, sindico, new MockHttpServletRequest());
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());

        ArgumentCaptor<BoletoJpaEntity> captor = ArgumentCaptor.forClass(BoletoJpaEntity.class);
        verify(boletoRepository).save(captor.capture());

        BoletoJpaEntity salvo = captor.getValue();
        assertEquals("PENDENTE", salvo.getStatus());
        assertNotNull(salvo.getLinhaDigitavel());
        assertTrue(salvo.getLinhaDigitavel().startsWith("34191.09008"));
        assertTrue(salvo.getLinhaDigitavel().endsWith("0000068550"));
        verify(auditService).registrarUsuario(eq(sindico), eq("BOLETO_EMITIDO_ADMIN"), eq("INFO"), any(), any());
    }

    @Test
    @DisplayName("Finanças Admin: bloqueia tentativa de injeção na emissão de boleto e gera alerta crítico")
    void deveRejeitarInjecaoNaEmissaoDeBoleto() {
        UsuarioJpaEntity sindico = new UsuarioJpaEntity();
        sindico.setId(UUID.randomUUID());
        sindico.setEmail("sindico@condominio.com");

        var dtoMalicioso = new FinancasController.EmitirBoletoRequestDTO(
                "Apto 101 <img src=x onerror=alert(1)>",
                "Carlos Silva",
                "11/2026",
                new BigDecimal("650.00"),
                LocalDate.now().plusDays(5)
        );

        var resposta = financasController.emitirBoleto(dtoMalicioso, sindico, new MockHttpServletRequest());
        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        verify(boletoRepository, never()).save(any());
        verify(auditService).registrarUsuario(eq(sindico), eq("TENTATIVA_INJECAO_EMISSAO_BOLETO"), eq("CRITICO"), any(), any());
    }

    @Test
    @DisplayName("Perfil Morador: retorna preferências padrão quando ausentes e persiste customizações de notificação")
    void deveRetornarPadraoESalvarPreferenciasDeNotificacaoDoMorador() {
        UUID moradorId = UUID.randomUUID();
        UsuarioJpaEntity morador = new UsuarioJpaEntity();
        morador.setId(moradorId);
        morador.setEmail("carlos@email.com");
        Authentication auth = new UsernamePasswordAuthenticationToken(morador, null, Collections.emptyList());

        when(preferenciaRepository.findById(moradorId)).thenReturn(Optional.empty());
        when(preferenciaRepository.save(any(PreferenciaNotificacaoJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var getPadrao = perfilController.getPreferenciasNotificacao(auth);
        assertEquals(HttpStatus.OK, getPadrao.getStatusCode());
        assertNotNull(getPadrao.getBody());
        assertTrue(getPadrao.getBody().encomendas_push());
        assertTrue(getPadrao.getBody().resumo_email());

        var novasPrefs = new PerfilController.PreferenciasNotificacaoDTO(
                true, false, true, true, false, true, false
        );
        var putResp = perfilController.salvarPreferenciasNotificacao(auth, novasPrefs, new MockHttpServletRequest());
        assertEquals(HttpStatus.OK, putResp.getStatusCode());
        assertNotNull(putResp.getBody());
        assertFalse(putResp.getBody().boletos_push());
        assertFalse(putResp.getBody().resumo_email());
        verify(auditService).registrarUsuario(eq(morador), eq("PREFERENCIAS_NOTIFICACAO_ATUALIZADAS"), eq("INFO"), any(), any());
    }

    @Test
    @DisplayName("Integrações Externas: Asaas e Firebase operam com contingência resiliente em modo local")
    void deveOperarComContingenciaSeguraEmModoLocal() {
        assertFalse(asaasServiceLocal.isIntegracaoAtiva());
        String linha = asaasServiceLocal.gerarBoletoReal(UUID.randomUUID(), new BigDecimal("520.00"), "12345678909", "Maria");
        assertTrue(linha.contains("0000052000"));

        FirebasePushNotificationServiceImpl firebaseLocal = new FirebasePushNotificationServiceImpl("");
        assertDoesNotThrow(() -> {
            firebaseLocal.inicializarSeConfigurado();
            firebaseLocal.enviarPush("fcm_token_exemplo_1234567890", "Aviso da Portaria", "Sua visita chegou.");
        });
    }
}

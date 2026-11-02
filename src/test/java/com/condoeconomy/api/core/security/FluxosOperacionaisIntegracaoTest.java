package com.condoeconomy.api.core.security;

import com.condoeconomy.api.application.gateway.CondominioRepository;
import com.condoeconomy.api.application.gateway.PedidoRepository;
import com.condoeconomy.api.application.gateway.ProdutoRepository;
import com.condoeconomy.api.application.service.NotificationService;
import com.condoeconomy.api.application.service.RotinasAgendadasService;
import com.condoeconomy.api.application.service.VotacaoService;
import com.condoeconomy.api.application.service.notification.FirebasePushNotificationServiceImpl;
import com.condoeconomy.api.application.service.payment.AsaasGatewayPagamentoServiceImpl;
import com.condoeconomy.api.application.usecase.CadastrarCondominioUseCase;
import com.condoeconomy.api.application.usecase.chamado.AtualizarStatusChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.CriarChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.EscalarChamadoUseCase;
import com.condoeconomy.api.application.usecase.chamado.ListarChamadosUseCase;
import com.condoeconomy.api.domain.entity.Condominio;
import com.condoeconomy.api.domain.entity.OpcaoVotacao;
import com.condoeconomy.api.domain.entity.Produto;
import com.condoeconomy.api.domain.entity.Votacao;
import com.condoeconomy.api.domain.entity.chamado.Chamado;
import com.condoeconomy.api.domain.enums.VotacaoStatus;
import com.condoeconomy.api.infrastructure.persistence.entity.PreferenciaNotificacaoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.financeiro.BoletoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.financeiro.DespesaJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.reserva.ReservaJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataMoradorAdicionalRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPetRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPreferenciaNotificacaoRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataVeiculoRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataBoletoRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataDespesaRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.reserva.SpringDataReservaRepository;
import com.condoeconomy.api.infrastructure.repository.VotacaoRepository;
import com.condoeconomy.api.infrastructure.repository.VotoRepository;
import com.condoeconomy.api.presentation.controller.CondominioController;
import com.condoeconomy.api.presentation.controller.FinancasController;
import com.condoeconomy.api.presentation.controller.admin.AdminCondoStoreController;
import com.condoeconomy.api.presentation.controller.chamado.ChamadoController;
import com.condoeconomy.api.presentation.controller.perfil.PerfilController;
import com.condoeconomy.api.presentation.dto.chamado.CriarChamadoRequestDTO;
import com.condoeconomy.api.presentation.dto.votacao.VotoRequestDTO;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    @DisplayName("Finanças Admin: lança despesa operacional, efetua baixa (pagamento) e lista despesas")
    void deveCadastrarPagarEListarDespesasOperacionaisComAuditoria() {
        UsuarioJpaEntity sindico = new UsuarioJpaEntity();
        sindico.setId(UUID.randomUUID());
        sindico.setEmail("sindico@condominio.com");

        var despesaDto = new FinancasController.SalvarDespesaRequestDTO(
                "Manutenção Preventiva de Elevadores",
                new BigDecimal("1450.00"),
                LocalDate.now().plusDays(15),
                "PENDENTE"
        );

        when(despesaRepository.save(any(DespesaJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var respCadastro = financasController.cadastrarDespesa(despesaDto, sindico, new MockHttpServletRequest());
        assertEquals(HttpStatus.CREATED, respCadastro.getStatusCode());
        assertNotNull(respCadastro.getBody());
        var corpoCadastro = (FinancasController.DespesaResponseDTO) respCadastro.getBody();
        assertEquals("PENDENTE", corpoCadastro.status());
        assertEquals("Manutenção Preventiva de Elevadores", corpoCadastro.descricao());

        UUID despesaId = corpoCadastro.id();
        DespesaJpaEntity entidadeSalva = new DespesaJpaEntity();
        entidadeSalva.setId(despesaId);
        entidadeSalva.setDescricao(corpoCadastro.descricao());
        entidadeSalva.setValor(corpoCadastro.valor());
        entidadeSalva.setDataVencimento(corpoCadastro.dataVencimento());
        entidadeSalva.setStatus("PENDENTE");

        when(despesaRepository.findById(despesaId)).thenReturn(Optional.of(entidadeSalva));

        var respPagamento = financasController.registrarPagamentoDespesa(despesaId, sindico, new MockHttpServletRequest());
        assertEquals(HttpStatus.OK, respPagamento.getStatusCode());
        assertNotNull(respPagamento.getBody());
        var corpoPagamento = (FinancasController.DespesaResponseDTO) respPagamento.getBody();
        assertEquals("PAGO", corpoPagamento.status());
        assertNotNull(corpoPagamento.dataPagamento());

        when(despesaRepository.findAllByOrderByDataVencimentoDesc()).thenReturn(List.of(entidadeSalva));
        var respLista = financasController.listarDespesas();
        assertEquals(HttpStatus.OK, respLista.getStatusCode());
        assertEquals(1, respLista.getBody().size());
    }

    @Test
    @DisplayName("Assembleia Virtual: bloqueia voto de morador inadimplente com base no Art. 1.335, III do Código Civil")
    void deveBloquearVotoDeMoradorInadimplenteConformeArt1335CodigoCivil() {
        VotacaoRepository votacaoRepo = mock(VotacaoRepository.class);
        VotoRepository votoRepo = mock(VotoRepository.class);
        SpringDataUsuarioRepository usuarioRepo = mock(SpringDataUsuarioRepository.class);
        SpringDataBoletoRepository boletoRepo = mock(SpringDataBoletoRepository.class);

        VotacaoService votacaoService = new VotacaoService(votacaoRepo, votoRepo, usuarioRepo, boletoRepo);

        UsuarioJpaEntity morador = new UsuarioJpaEntity();
        morador.setId(UUID.randomUUID());
        morador.setEmail("morador.inadimplente@condominio.com");
        morador.setApartamento("302");

        BoletoJpaEntity boletoVencido = new BoletoJpaEntity();
        boletoVencido.setId(UUID.randomUUID());
        boletoVencido.setStatus("VENCIDO");
        boletoVencido.setDataVencimento(LocalDate.now().minusDays(5));
        boletoVencido.setUnidadeTexto("Apto 302");

        when(usuarioRepo.findByEmail("morador.inadimplente@condominio.com")).thenReturn(Optional.of(morador));
        when(boletoRepo.findByUnidadeTextoContainingIgnoreCaseOrderByDataVencimentoDesc("302"))
                .thenReturn(List.of(boletoVencido));

        assertFalse(votacaoService.verificarAdimplencia(morador));

        UUID votacaoId = UUID.randomUUID();
        Votacao votacao = new Votacao();
        votacao.setId(votacaoId);
        votacao.setStatus(VotacaoStatus.ABERTA);
        votacao.setDataEncerramento(LocalDateTime.now().plusDays(2));

        OpcaoVotacao opcao = new OpcaoVotacao();
        UUID opcaoId = UUID.randomUUID();
        opcao.setId(opcaoId);
        opcao.setVotacao(votacao);
        votacao.setOpcoes(List.of(opcao));

        when(votacaoRepo.findById(votacaoId)).thenReturn(Optional.of(votacao));

        VotoRequestDTO votoDto = new VotoRequestDTO();
        votoDto.setOpcaoId(opcaoId);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                votacaoService.registrarVoto(votacaoId, votoDto, "morador.inadimplente@condominio.com")
        );

        assertTrue(ex.getMessage().contains("adimplente"));
        verify(votoRepo, never()).save(any());
    }

    @Test
    @DisplayName("Multi-Tenancy: Super Admin lista todos os condomínios provisionados na plataforma")
    void deveListarCondominiosProvisionadosParaSuperAdmin() {
        CondominioRepository condRepo = mock(CondominioRepository.class);
        CadastrarCondominioUseCase useCase = mock(CadastrarCondominioUseCase.class);
        CondominioController condController = new CondominioController(useCase, condRepo);

        Condominio c1 = new Condominio(UUID.randomUUID(), "Residencial Jardins", "12345678000199", "resjardins");
        when(condRepo.listarTodos()).thenReturn(List.of(c1));

        var resposta = condController.listar();
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertEquals(1, resposta.getBody().size());
        assertEquals("Residencial Jardins", resposta.getBody().get(0).nome());
        assertEquals("resjardins", resposta.getBody().get(0).tenantId());
    }

    @Test
    @DisplayName("Ouvidoria: cria chamado com foto anexa segura e rejeita payload malicioso")
    void deveCriarChamadoComFotoAnexoSeguroERejeitarPayloadInseguro() {
        ListarChamadosUseCase listarUseCase = mock(ListarChamadosUseCase.class);
        AtualizarStatusChamadoUseCase atualizarUseCase = mock(AtualizarStatusChamadoUseCase.class);
        CriarChamadoUseCase criarUseCase = mock(CriarChamadoUseCase.class);
        EscalarChamadoUseCase escalarUseCase = mock(EscalarChamadoUseCase.class);

        ChamadoController chamadoController = new ChamadoController(
                listarUseCase, atualizarUseCase, criarUseCase, escalarUseCase
        );

        UsuarioJpaEntity morador = new UsuarioJpaEntity();
        morador.setId(UUID.randomUUID());
        morador.setNome("Carlos Silva");
        morador.setApartamento("101");
        morador.setBloco("A");
        morador.setPapel("ROLE_MORADOR");

        Authentication auth = new UsernamePasswordAuthenticationToken(morador, null, Collections.emptyList());

        String fotoSegura = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD";
        Chamado chamadoCriado = Chamado.abrir("Apto 101 - Bloco A", "Carlos Silva",
                Chamado.CategoriaChamado.MANUTENCAO, "Vazamento no banheiro", "Infiltração no teto", fotoSegura);

        when(criarUseCase.executar(eq("Apto 101 - Bloco A"), eq("Carlos Silva"), eq("MANUTENCAO"),
                eq("Vazamento no banheiro"), eq("Infiltração no teto"), eq(fotoSegura)))
                .thenReturn(chamadoCriado);

        var dtoValido = new CriarChamadoRequestDTO(
                null, null, "MANUTENCAO", "Vazamento no banheiro", "Infiltração no teto", fotoSegura
        );

        var respValida = chamadoController.criarChamado(dtoValido, auth);
        assertEquals(HttpStatus.OK, respValida.getStatusCode());
        assertNotNull(respValida.getBody());
        assertEquals(fotoSegura, respValida.getBody().fotoAnexo());

        var dtoInseguro = new CriarChamadoRequestDTO(
                null, null, "MANUTENCAO", "Vazamento", "Infiltração", "javascript:alert(1)"
        );

        assertThrows(IllegalArgumentException.class, () ->
                chamadoController.criarChamado(dtoInseguro, auth)
        );
    }

    @Test
    @DisplayName("Rotinas Agendadas: expira boletos, encerra votações e cancela reservas pendentes expiradas")
    void deveExecutarRotinasAgendadasDeBoletosVotacoesEReservas() {
        var mockBoletoRepo = mock(SpringDataBoletoRepository.class);
        var mockVotacaoRepo = mock(VotacaoRepository.class);
        var mockReservaRepo = mock(SpringDataReservaRepository.class);
        var mockNotificationService = mock(NotificationService.class);

        var rotinas = new RotinasAgendadasService(
                mockBoletoRepo,
                mockVotacaoRepo,
                mockReservaRepo,
                mockNotificationService
        );

        // 1. Boletos vencidos
        var boletoAtrasado = new BoletoJpaEntity(
                UUID.randomUUID(), "Apto 101", "João", "10/2026",
                BigDecimal.valueOf(500), LocalDate.now().minusDays(5), null,
                "PENDENTE", "123456", "url"
        );
        when(mockBoletoRepo.findByStatusAndDataVencimentoBefore(eq("PENDENTE"), any(LocalDate.class)))
                .thenReturn(List.of(boletoAtrasado));

        int boletosAtualizados = rotinas.atualizarBoletosVencidos();
        assertEquals(1, boletosAtualizados);
        assertEquals("VENCIDO", boletoAtrasado.getStatus());
        verify(mockBoletoRepo).save(boletoAtrasado);

        // 2. Votações expiradas
        var votacaoExpirada = new Votacao();
        votacaoExpirada.setId(UUID.randomUUID());
        votacaoExpirada.setTitulo("Reforma da Fachada");
        votacaoExpirada.setStatus(VotacaoStatus.ABERTA);
        votacaoExpirada.setDataEncerramento(LocalDateTime.now().minusHours(1));

        when(mockVotacaoRepo.findByStatusAndDataEncerramentoBefore(eq(VotacaoStatus.ABERTA), any(LocalDateTime.class)))
                .thenReturn(List.of(votacaoExpirada));

        int votacoesEncerradas = rotinas.encerrarVotacoesExpiradas();
        assertEquals(1, votacoesEncerradas);
        assertEquals(VotacaoStatus.ENCERRADA, votacaoExpirada.getStatus());
        verify(mockVotacaoRepo).save(votacaoExpirada);

        // 3. Reservas não respondidas com data do evento ultrapassada
        var reservaExpirada = new ReservaJpaEntity();
        reservaExpirada.setId(UUID.randomUUID());
        reservaExpirada.setUnidadeTexto("Apto 202");
        reservaExpirada.setDataReserva(LocalDate.now().minusDays(1));
        reservaExpirada.setStatus("PENDENTE_APROVACAO");

        when(mockReservaRepo.findByStatusAndDataReservaBefore(eq("PENDENTE_APROVACAO"), any(LocalDate.class)))
                .thenReturn(List.of(reservaExpirada));

        int reservasCanceladas = rotinas.expirarReservasNaoRespondidas();
        assertEquals(1, reservasCanceladas);
        assertEquals("REJEITADA", reservaExpirada.getStatus());
        assertTrue(reservaExpirada.getMotivoRejeicao().contains("Cancelada automaticamente"));
        verify(mockReservaRepo).save(reservaExpirada);
        verify(mockNotificationService).notifyReservasUpdate();
    }

    @Test
    @DisplayName("Finanças: gera balancete mensal consolidado com receitas, despesas e taxa de adimplência")
    void deveGerarBalanceteMensalConsolidadoComReceitasEDespesas() {
        int mes = 11;
        int ano = 2026;

        var b1 = new BoletoJpaEntity(
                UUID.randomUUID(), "Apto 101", "Ana Silva", "11/2026",
                BigDecimal.valueOf(400.00), LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 8),
                "PAGO", "111", null
        );
        var b2 = new BoletoJpaEntity(
                UUID.randomUUID(), "Apto 102", "Bruno Lima", "11/2026",
                BigDecimal.valueOf(400.00), LocalDate.of(2026, 11, 10), null,
                "VENCIDO", "222", null
        );
        var b3 = new BoletoJpaEntity(
                UUID.randomUUID(), "Apto 103", "Carla Souza", "11/2026",
                BigDecimal.valueOf(400.00), LocalDate.of(2026, 11, 15), null,
                "PENDENTE", "333", null
        );

        when(boletoRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        var d1 = new DespesaJpaEntity();
        d1.setId(UUID.randomUUID());
        d1.setDescricao("Manutenção de Elevadores");
        d1.setValor(BigDecimal.valueOf(250.00));
        d1.setDataVencimento(LocalDate.of(2026, 11, 5));
        d1.setDataPagamento(LocalDate.of(2026, 11, 5));
        d1.setStatus("PAGO");

        when(despesaRepository.findAll()).thenReturn(List.of(d1));

        var resp = financasController.gerarBalancete(mes, ano);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        var balancete = resp.getBody();
        assertNotNull(balancete);

        assertEquals(mes, balancete.mes());
        assertEquals(ano, balancete.ano());
        assertEquals(BigDecimal.valueOf(400.00), balancete.totalReceitas());
        assertEquals(BigDecimal.valueOf(250.00), balancete.totalDespesas());
        assertEquals(BigDecimal.valueOf(150.00), balancete.saldoOperacional());
        assertEquals(3, balancete.totalBoletosEmitidos());
        assertEquals(1, balancete.totalBoletosPagos());
        assertEquals(1, balancete.receitas().size());
        assertEquals(1, balancete.despesas().size());
    }
}

package com.condoeconomy.api.presentation.controller.admin;

import com.condoeconomy.api.core.security.SecurityAuditService;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@PreAuthorize("hasAnyRole('ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class AdminUsuarioController {

    private static final Set<String> PAPEIS_PERMITIDOS_SINDICO = Set.of("ROLE_MORADOR", "ROLE_PORTEIRO");
    private static final Set<String> PAPEIS_PERMITIDOS_SUPER_ADMIN = Set.of(
            "ROLE_MORADOR", "ROLE_PORTEIRO", "ROLE_SINDICO", "ROLE_ADMIN", "ROLE_SUPER_ADMIN"
    );

    private final SpringDataUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditService auditService;

    public AdminUsuarioController(SpringDataUsuarioRepository usuarioRepository,
                                  PasswordEncoder passwordEncoder,
                                  SecurityAuditService auditService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public record UsuarioAdminResponseDTO(
            UUID id,
            String nome,
            String email,
            String papel,
            String telefone,
            String apartamento,
            String bloco,
            boolean ativo
    ) {
        public static UsuarioAdminResponseDTO fromEntity(UsuarioJpaEntity u) {
            return new UsuarioAdminResponseDTO(
                    u.getId(),
                    u.getNome(),
                    u.getEmail(),
                    u.getPapel(),
                    u.getTelefone(),
                    u.getApartamento(),
                    u.getBloco(),
                    u.isAtivo()
            );
        }
    }

    public record CriarUsuarioAdminRequestDTO(
            @NotBlank(message = "O nome completo é obrigatório")
            @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
            String nome,

            @NotBlank(message = "O e-mail é obrigatório")
            @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
            @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Formato de e-mail inválido"
            )
            String email,

            @NotBlank(message = "A senha provisória é obrigatória")
            @Size(min = 8, max = 128, message = "A senha provisória deve ter entre 8 e 128 caracteres")
            String senhaInicial,

            @NotBlank(message = "O perfil de acesso é obrigatório")
            String papel,

            @Size(max = 30, message = "Telefone inválido")
            String telefone,

            @Size(max = 20, message = "Apartamento inválido")
            String apartamento,

            @Size(max = 20, message = "Bloco inválido")
            String bloco
    ) {}

    public record AtualizarStatusUsuarioDTO(boolean ativo) {}

    public record ItemErroImportacaoDTO(
            int linha,
            String email,
            String motivo
    ) {}

    public record MoradorImportadoResumoDTO(
            UUID id,
            String nome,
            String email,
            String apartamento,
            String bloco,
            String telefone
    ) {}

    public record ImportacaoMoradoresResultadoDTO(
            int totalProcessados,
            int sucessoCount,
            int falhaCount,
            List<MoradorImportadoResumoDTO> importados,
            List<ItemErroImportacaoDTO> erros
    ) {}

    @GetMapping
    public ResponseEntity<Map<String, Object>> listarUsuarios(
            @RequestParam(required = false) String papel,
            @RequestParam(required = false) String busca,
            @AuthenticationPrincipal UsuarioJpaEntity gestorAutenticado) {

        if (papel != null && !papel.isBlank()) {
            SecuritySanitizer.validarTextoSeguro(papel, "papel");
        }
        if (busca != null && !busca.isBlank()) {
            SecuritySanitizer.validarTextoSeguro(busca, "busca");
        }

        boolean isSuperAdmin = gestorAutenticado != null && "ROLE_SUPER_ADMIN".equals(gestorAutenticado.getPapel());
        List<UsuarioJpaEntity> base = usuarioRepository.findAllByOrderByNomeAsc().stream()
                .filter(u -> isSuperAdmin || !"ROLE_SUPER_ADMIN".equals(u.getPapel()))
                .toList();

        long total = base.size();
        long ativos = base.stream().filter(UsuarioJpaEntity::isAtivo).count();
        long bloqueados = total - ativos;
        long moradores = base.stream().filter(u -> "ROLE_MORADOR".equals(u.getPapel())).count();
        long portaria = base.stream().filter(u -> "ROLE_PORTEIRO".equals(u.getPapel())).count();

        String filtroPapel = papel != null ? papel.trim().toUpperCase(Locale.ROOT) : "";
        String termoBusca = busca != null ? busca.trim().toLowerCase(Locale.ROOT) : "";

        List<UsuarioAdminResponseDTO> filtrados = base.stream()
                .filter(u -> filtroPapel.isEmpty() || filtroPapel.equalsIgnoreCase(u.getPapel()))
                .filter(u -> {
                    if (termoBusca.isEmpty()) return true;
                    return contem(u.getNome(), termoBusca)
                            || contem(u.getEmail(), termoBusca)
                            || contem(u.getApartamento(), termoBusca)
                            || contem(u.getBloco(), termoBusca);
                })
                .map(UsuarioAdminResponseDTO::fromEntity)
                .toList();

        Map<String, Long> resumo = new LinkedHashMap<>();
        resumo.put("total", total);
        resumo.put("ativos", ativos);
        resumo.put("bloqueados", bloqueados);
        resumo.put("moradores", moradores);
        resumo.put("portaria", portaria);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("resumo", resumo);
        resposta.put("usuarios", filtrados);
        return ResponseEntity.ok(resposta);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> cadastrarUsuario(
            @RequestBody @Valid CriarUsuarioAdminRequestDTO dto,
            @AuthenticationPrincipal UsuarioJpaEntity gestorAutenticado,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();

        try {
            SecuritySanitizer.validarTextoSeguro(dto.nome(), "nome");
            SecuritySanitizer.validarTextoSeguro(dto.email(), "email");
            SecuritySanitizer.validarTextoSeguro(dto.papel(), "papel");
            SecuritySanitizer.validarTextoSeguro(dto.telefone(), "telefone");
            SecuritySanitizer.validarTextoSeguro(dto.apartamento(), "apartamento");
            SecuritySanitizer.validarTextoSeguro(dto.bloco(), "bloco");
            if (SecuritySanitizer.contemPadroesMaliciosos(dto.senhaInicial())) {
                throw new IllegalArgumentException("A senha provisória contém padrões não permitidos.");
            }
        } catch (IllegalArgumentException e) {
            auditService.registrarUsuario(gestorAutenticado, "TENTATIVA_INJECAO_CADASTRO_USUARIO", "CRITICO", ip, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }

        String papelNormalizado = dto.papel().trim().toUpperCase(Locale.ROOT);
        if (!papelNormalizado.startsWith("ROLE_")) {
            papelNormalizado = "ROLE_" + papelNormalizado;
        }

        boolean isSuperAdmin = gestorAutenticado != null && "ROLE_SUPER_ADMIN".equals(gestorAutenticado.getPapel());
        Set<String> papeisPermitidos = isSuperAdmin ? PAPEIS_PERMITIDOS_SUPER_ADMIN : PAPEIS_PERMITIDOS_SINDICO;

        if (!papeisPermitidos.contains(papelNormalizado)) {
            auditService.registrarUsuario(gestorAutenticado, "TENTATIVA_ESCALACAO_PRIVILEGIO_CADASTRO", "CRITICO", ip,
                    "Tentativa bloqueada de criar usuário com papel restrito: " + papelNormalizado);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Permissão insuficiente para atribuir o perfil solicitado."));
        }

        String emailLimpo = SecuritySanitizer.sanitizar(dto.email()).toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(emailLimpo)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Já existe um usuário cadastrado com este e-mail."));
        }

        if ("ROLE_MORADOR".equals(papelNormalizado)
                && (dto.apartamento() == null || dto.apartamento().isBlank())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Informe a unidade/apartamento para o cadastro de morador."));
        }

        UsuarioJpaEntity novo = new UsuarioJpaEntity();
        novo.setId(UUID.randomUUID());
        novo.setNome(SecuritySanitizer.sanitizar(dto.nome()));
        novo.setEmail(emailLimpo);
        novo.setSenha(passwordEncoder.encode(dto.senhaInicial()));
        novo.setPapel(papelNormalizado);
        novo.setTelefone(dto.telefone() != null && !dto.telefone().isBlank() ? SecuritySanitizer.sanitizar(dto.telefone()) : null);
        novo.setApartamento(dto.apartamento() != null && !dto.apartamento().isBlank() ? SecuritySanitizer.sanitizar(dto.apartamento()) : null);
        novo.setBloco(dto.bloco() != null && !dto.bloco().isBlank() ? SecuritySanitizer.sanitizar(dto.bloco()) : null);
        novo.setTokenVersion(1);
        novo.setAtivo(true);

        UsuarioJpaEntity salvo = usuarioRepository.save(novo);

        auditService.registrarUsuario(gestorAutenticado, "USUARIO_CADASTRADO_ADMIN", "INFO", ip,
                "Novo usuário cadastrado: '" + salvo.getEmail() + "' (" + salvo.getPapel() + ").");

        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioAdminResponseDTO.fromEntity(salvo));
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @Transactional
    public ResponseEntity<?> alterarStatusConta(
            @PathVariable UUID id,
            @RequestBody AtualizarStatusUsuarioDTO dto,
            @AuthenticationPrincipal UsuarioJpaEntity gestorAutenticado,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();

        if (gestorAutenticado != null && gestorAutenticado.getId() != null && gestorAutenticado.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Você não pode bloquear sua própria conta administrativa."));
        }

        var optAlvo = usuarioRepository.findById(id);
        if (optAlvo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuário não encontrado."));
        }

        UsuarioJpaEntity alvo = optAlvo.get();
        boolean isSuperAdmin = gestorAutenticado != null && "ROLE_SUPER_ADMIN".equals(gestorAutenticado.getPapel());
        if (!isSuperAdmin && ("ROLE_SUPER_ADMIN".equals(alvo.getPapel()) || "ROLE_ADMIN".equals(alvo.getPapel()))) {
            auditService.registrarUsuario(gestorAutenticado, "TENTATIVA_ALTERACAO_CONTA_SUPERIOR", "CRITICO", ip,
                    "Tentativa bloqueada de alterar status de conta administrativa superior: " + alvo.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Permissão insuficiente para alterar o status desta conta administrativa."));
        }

        alvo.setAtivo(dto.ativo());
        if (!dto.ativo()) {
            // Revoga imediatamente tokens JWT e cookies ativos do usuário bloqueado
            alvo.setTokenVersion(alvo.getTokenVersion() + 1);
        }

        UsuarioJpaEntity atualizado = usuarioRepository.save(alvo);

        auditService.registrarUsuario(
                gestorAutenticado,
                dto.ativo() ? "CONTA_REATIVADA_ADMIN" : "CONTA_BLOQUEADA_ADMIN",
                dto.ativo() ? "INFO" : "ALERTA",
                ip,
                (dto.ativo() ? "Conta reativada: " : "Conta bloqueada e sessões revogadas: ") + atualizado.getEmail()
        );

        return ResponseEntity.ok(UsuarioAdminResponseDTO.fromEntity(atualizado));
    }

    @GetMapping(value = "/modelo-csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> baixarModeloCsv() {
        String modelo = "Nome;Email;Apartamento;Bloco;Telefone;SenhaProvisoria\n" +
                "Carlos Oliveira;carlos.oliveira@email.com;101;Bloco A;11999990001;Senha@123\n" +
                "Mariana Santos;mariana.santos@email.com;102;Bloco A;11999990002;Senha@123\n" +
                "Lucas Ferreira;lucas.ferreira@email.com;201;Bloco B;11999990003;Senha@123\n";
        byte[] bytes = modelo.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"modelo_importacao_moradores.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(bytes);
    }

    @PostMapping(value = "/importar-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> importarMoradoresCsv(
            @RequestParam("arquivo") MultipartFile arquivo,
            @AuthenticationPrincipal UsuarioJpaEntity gestorAutenticado,
            HttpServletRequest request) {

        String ip = request.getRemoteAddr();

        if (arquivo == null || arquivo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Nenhum arquivo CSV foi enviado."));
        }

        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal != null && !nomeOriginal.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Formato de arquivo inválido. Apenas arquivos .csv são suportados."));
        }

        List<UsuarioJpaEntity> paraSalvar = new ArrayList<>();
        List<MoradorImportadoResumoDTO> importados = new ArrayList<>();
        List<ItemErroImportacaoDTO> erros = new ArrayList<>();
        Set<String> emailsNoLote = new HashSet<>();

        int linhaNum = 0;
        int totalLinhas = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            String linha = reader.readLine();
            if (linha != null && linha.startsWith("\uFEFF")) {
                linha = linha.substring(1);
            }

            if (linha == null || linha.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "O arquivo CSV está vazio."));
            }

            String delimitador = linha.contains(";") ? ";" : ",";

            boolean primeiraLinhaCabecalho = linha.toLowerCase(Locale.ROOT).contains("nome")
                    || linha.toLowerCase(Locale.ROOT).contains("email");

            if (!primeiraLinhaCabecalho) {
                linhaNum++;
                totalLinhas++;
                processarLinhaCsv(linha, delimitador, linhaNum, paraSalvar, importados, erros, emailsNoLote);
            }

            while ((linha = reader.readLine()) != null) {
                linhaNum++;
                if (linha.trim().isEmpty()) continue;
                totalLinhas++;
                processarLinhaCsv(linha, delimitador, linhaNum, paraSalvar, importados, erros, emailsNoLote);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Erro ao processar arquivo CSV: " + e.getMessage()));
        }

        if (!paraSalvar.isEmpty()) {
            usuarioRepository.saveAll(paraSalvar);
            auditService.registrarUsuario(
                    gestorAutenticado,
                    "IMPORTACAO_MORADORES_LOTE",
                    "INFO",
                    ip,
                    "Importação de moradores via CSV concluída com sucesso: " + paraSalvar.size() + " moradores cadastrados."
            );
        }

        return ResponseEntity.ok(new ImportacaoMoradoresResultadoDTO(
                totalLinhas,
                importados.size(),
                erros.size(),
                importados,
                erros
        ));
    }

    private void processarLinhaCsv(
            String linha,
            String delimitador,
            int linhaNum,
            List<UsuarioJpaEntity> paraSalvar,
            List<MoradorImportadoResumoDTO> importados,
            List<ItemErroImportacaoDTO> erros,
            Set<String> emailsNoLote
    ) {
        String[] colunas = linha.split(delimitador, -1);
        if (colunas.length < 2) {
            erros.add(new ItemErroImportacaoDTO(linhaNum, "-", "Linha incompleta ou delimitador incompatível (use ponto e vírgula ou vírgula)."));
            return;
        }

        String nome = SecuritySanitizer.sanitizarCsvFormula(limparAspas(colunas.length > 0 ? colunas[0] : ""));
        String email = limparAspas(colunas.length > 1 ? colunas[1] : "");
        String apartamento = SecuritySanitizer.sanitizarCsvFormula(limparAspas(colunas.length > 2 ? colunas[2] : ""));
        String bloco = SecuritySanitizer.sanitizarCsvFormula(limparAspas(colunas.length > 3 ? colunas[3] : ""));
        String telefone = SecuritySanitizer.sanitizarCsvFormula(limparAspas(colunas.length > 4 ? colunas[4] : ""));
        String senha = limparAspas(colunas.length > 5 ? colunas[5] : "");

        if (nome.isBlank()) {
            erros.add(new ItemErroImportacaoDTO(linhaNum, email, "Nome do morador é obrigatório."));
            return;
        }

        if (email.isBlank() || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            erros.add(new ItemErroImportacaoDTO(linhaNum, email, "E-mail inválido ou em branco."));
            return;
        }

        String emailLower = email.toLowerCase(Locale.ROOT);
        if (emailsNoLote.contains(emailLower)) {
            erros.add(new ItemErroImportacaoDTO(linhaNum, email, "E-mail duplicado dentro do próprio arquivo CSV."));
            return;
        }

        if (usuarioRepository.existsByEmailIgnoreCase(emailLower)) {
            erros.add(new ItemErroImportacaoDTO(linhaNum, email, "E-mail já cadastrado no sistema."));
            return;
        }

        if (apartamento.isBlank()) {
            erros.add(new ItemErroImportacaoDTO(linhaNum, email, "Unidade/Apartamento é obrigatório para cadastro de morador."));
            return;
        }

        try {
            SecuritySanitizer.validarTextoSeguro(nome, "nome");
            SecuritySanitizer.validarTextoSeguro(email, "email");
            SecuritySanitizer.validarTextoSeguro(apartamento, "apartamento");
            if (!bloco.isBlank()) SecuritySanitizer.validarTextoSeguro(bloco, "bloco");
            if (!telefone.isBlank()) SecuritySanitizer.validarTextoSeguro(telefone, "telefone");
            if (!senha.isBlank() && SecuritySanitizer.contemPadroesMaliciosos(senha)) {
                erros.add(new ItemErroImportacaoDTO(linhaNum, email, "Senha provisória com caracteres suspeitos."));
                return;
            }
        } catch (IllegalArgumentException e) {
            erros.add(new ItemErroImportacaoDTO(linhaNum, email, "Caracteres inseguros detectados: " + e.getMessage()));
            return;
        }

        emailsNoLote.add(emailLower);

        String senhaFinal = (!senha.isBlank() && senha.length() >= 6) ? senha : "HabitOS@" + (100 + (int)(Math.random() * 900));

        UUID id = UUID.randomUUID();
        UsuarioJpaEntity novo = new UsuarioJpaEntity();
        novo.setId(id);
        novo.setNome(SecuritySanitizer.sanitizar(nome));
        novo.setEmail(emailLower);
        novo.setSenha(passwordEncoder.encode(senhaFinal));
        novo.setPapel("ROLE_MORADOR");
        novo.setApartamento(SecuritySanitizer.sanitizar(apartamento));
        novo.setBloco(!bloco.isBlank() ? SecuritySanitizer.sanitizar(bloco) : null);
        novo.setTelefone(!telefone.isBlank() ? SecuritySanitizer.sanitizar(telefone) : null);
        novo.setTokenVersion(1);
        novo.setAtivo(true);

        paraSalvar.add(novo);
        importados.add(new MoradorImportadoResumoDTO(id, novo.getNome(), novo.getEmail(), novo.getApartamento(), novo.getBloco(), novo.getTelefone()));
    }

    private static String limparAspas(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private static boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(termo);
    }
}

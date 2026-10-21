package com.condoeconomy.api.presentation.controller.perfil;

import com.condoeconomy.api.core.security.SecurityAuditService;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.core.security.TokenService;
import com.condoeconomy.api.infrastructure.persistence.entity.AuditoriaSegurancaJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.MoradorAdicionalJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.PetJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.VeiculoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataMoradorAdicionalRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPetRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataVeiculoRepository;
import com.condoeconomy.api.presentation.controller.AuthController;
import com.condoeconomy.api.presentation.dto.perfil.AdicionarMoradorRequestDTO;
import com.condoeconomy.api.presentation.dto.perfil.AdicionarPetRequestDTO;
import com.condoeconomy.api.presentation.dto.perfil.AdicionarVeiculoRequestDTO;
import com.condoeconomy.api.presentation.dto.perfil.AlterarSenhaRequestDTO;
import com.condoeconomy.api.presentation.dto.perfil.AtualizarDadosPessoaisRequestDTO;
import com.condoeconomy.api.presentation.dto.perfil.UsuarioResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private static final Logger log = LoggerFactory.getLogger(PerfilController.class);

    private final SpringDataVeiculoRepository veiculoRepository;
    private final SpringDataPetRepository petRepository;
    private final SpringDataMoradorAdicionalRepository moradorAdicionalRepository;
    private final SpringDataUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SecurityAuditService auditService;

    private UUID getLoggedUserId(Authentication authentication) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) authentication.getPrincipal();
        return user.getId();
    }

    // --- SEGURANÇA / STEP-UP AUTH, REVOGAÇÃO DE SESSÕES & TRILHA DE AUDITORIA ---

    @GetMapping("/seguranca/historico")
    public ResponseEntity<List<AuditoriaSegurancaJpaEntity>> listarHistoricoSeguranca(Authentication auth) {
        return ResponseEntity.ok(auditService.listarPorUsuario(getLoggedUserId(auth)));
    }

    @PutMapping("/seguranca/alterar-senha")
    public ResponseEntity<?> alterarSenha(
            Authentication auth,
            @RequestBody @Valid AlterarSenhaRequestDTO dto,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return usuarioRepository.findById(getLoggedUserId(auth))
                .map(usuario -> {
                    // 1. Step-up Auth: validação estrita da senha atual
                    if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
                        log.warn("[AUDITORIA DE SEGURANÇA] Falha de Step-up Auth na troca de senha para o usuário '{}'", usuario.getEmail());
                        auditService.registrarUsuario(usuario, "FALHA_STEP_UP_AUTH", "ALERTA", ip,
                                "Tentativa de troca de senha bloqueada: senha atual incorreta.");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "A senha atual informada está incorreta."));
                    }

                    // 2. Impede reuso imediato da mesma senha
                    if (passwordEncoder.matches(dto.novaSenha(), usuario.getSenha())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", "A nova senha deve ser diferente da senha atual."));
                    }

                    // 3. Sanitização contra caracteres de controle
                    SecuritySanitizer.validarTextoSeguro(dto.novaSenha(), "novaSenha");

                    // 4. Criptografia BCrypt da nova credencial
                    usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));

                    // 5. Invalidação imediata de todas as outras sessões (Token Kill Switch)
                    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
                    usuarioRepository.save(usuario);

                    log.info("[AUDITORIA DE SEGURANÇA] Senha alterada e sessões anteriores revogadas (tokenVersion={}) para o usuário '{}'",
                            usuario.getTokenVersion(), usuario.getEmail());
                    auditService.registrarUsuario(usuario, "SENHA_ALTERADA", "INFO", ip,
                            "Senha alterada com Step-Up Auth e sessões anteriores revogadas (tokenVersion=" + usuario.getTokenVersion() + ").");

                    // 6. Emissão de novo token e atualização do Cookie HttpOnly para manter a sessão do dispositivo atual
                    String novoToken = tokenService.generateToken(usuario);
                    ResponseCookie sessionCookie = AuthController.criarCookieSessao(novoToken, request.isSecure(), Duration.ofHours(2));

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                            .body(Map.of(
                                    "message", "Senha alterada com sucesso! Todas as outras sessões foram desconectadas por segurança.",
                                    "token", novoToken
                            ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/seguranca/encerrar-sessoes")
    public ResponseEntity<?> encerrarOutrasSessoes(Authentication auth, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return usuarioRepository.findById(getLoggedUserId(auth))
                .map(usuario -> {
                    // Incrementa tokenVersion, invalidando instantaneamente qualquer JWT emitido anteriormente
                    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
                    usuarioRepository.save(usuario);

                    log.info("[AUDITORIA DE SEGURANÇA] Kill Switch acionado: todas as sessões anteriores revogadas (tokenVersion={}) para o usuário '{}'",
                            usuario.getTokenVersion(), usuario.getEmail());
                    auditService.registrarUsuario(usuario, "SESSOES_REVOGADAS_KILL_SWITCH", "ALERTA", ip,
                            "Kill Switch acionado pelo titular: todas as outras sessões foram desconectadas (tokenVersion=" + usuario.getTokenVersion() + ").");

                    // Emite um novo token válido exclusivamente para a sessão ativa e atualiza o Cookie HttpOnly
                    String novoToken = tokenService.generateToken(usuario);
                    ResponseCookie sessionCookie = AuthController.criarCookieSessao(novoToken, request.isSecure(), Duration.ofHours(2));

                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                            .body(Map.of(
                                    "message", "Todas as outras sessões foram encerradas com sucesso.",
                                    "token", novoToken
                            ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- LGPD ART. 18: DIREITO DE ACESSO E PORTABILIDADE DOS DADOS PESSOAIS ---

    @GetMapping("/lgpd/exportar-dados")
    public ResponseEntity<?> exportarDadosLgpd(Authentication auth, HttpServletRequest request) {
        UUID userId = getLoggedUserId(auth);
        return usuarioRepository.findById(userId)
                .map(usuario -> {
                    auditService.registrarUsuario(usuario, "EXPORTACAO_DADOS_LGPD", "INFO", request.getRemoteAddr(),
                            "Relatório de portabilidade de dados pessoais (Art. 18 LGPD) gerado pelo titular.");

                    Map<String, Object> relatorio = new LinkedHashMap<>();
                    relatorio.put("baseLegal", "Lei Geral de Proteção de Dados (Lei nº 13.709/2018) - Art. 18 (Acesso e Portabilidade)");
                    relatorio.put("emitidoEm", LocalDateTime.now().toString());
                    relatorio.put("titular", UsuarioResponseDTO.fromEntity(usuario));
                    relatorio.put("veiculosCadastrados", veiculoRepository.findByUsuarioId(userId));
                    relatorio.put("petsCadastrados", petRepository.findByUsuarioId(userId));
                    relatorio.put("moradoresDependentes", moradorAdicionalRepository.findByUsuarioId(userId));
                    relatorio.put("trilhaAuditoriaSeguranca", auditService.listarPorUsuario(userId));

                    return ResponseEntity.ok(relatorio);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- DADOS PESSOAIS ---
    @GetMapping("/dados")
    public ResponseEntity<UsuarioResponseDTO> getDadosPessoais(Authentication auth) {
        return usuarioRepository.findById(getLoggedUserId(auth))
                .map(UsuarioResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/dados")
    public ResponseEntity<UsuarioResponseDTO> updateDadosPessoais(
            Authentication auth,
            @RequestBody @Valid AtualizarDadosPessoaisRequestDTO dados,
            HttpServletRequest request) {
        SecuritySanitizer.validarTextoSeguro(dados.nome(), "nome");
        SecuritySanitizer.validarTextoSeguro(dados.telefone(), "telefone");
        SecuritySanitizer.validarUrlFotoSegura(dados.foto());

        return usuarioRepository.findById(getLoggedUserId(auth))
                .map(usuario -> {
                    usuario.setNome(SecuritySanitizer.sanitizar(dados.nome()));
                    usuario.setTelefone(dados.telefone() != null ? SecuritySanitizer.sanitizar(dados.telefone()) : null);
                    usuario.setFoto(dados.foto());
                    UsuarioJpaEntity saved = usuarioRepository.save(usuario);
                    auditService.registrarUsuario(saved, "DADOS_PESSOAIS_ATUALIZADOS", "INFO", request.getRemoteAddr(),
                            "Dados cadastrais do perfil atualizados pelo titular.");
                    return ResponseEntity.ok(UsuarioResponseDTO.fromEntity(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- VEÍCULOS ---
    @GetMapping("/veiculos")
    public ResponseEntity<List<VeiculoJpaEntity>> listarVeiculos(Authentication auth) {
        return ResponseEntity.ok(veiculoRepository.findByUsuarioId(getLoggedUserId(auth)));
    }

    @PostMapping("/veiculos")
    public ResponseEntity<VeiculoJpaEntity> addVeiculo(Authentication auth, @RequestBody @Valid AdicionarVeiculoRequestDTO dto) {
        SecuritySanitizer.validarTextoSeguro(dto.placa(), "placa");
        SecuritySanitizer.validarTextoSeguro(dto.modelo(), "modelo");
        SecuritySanitizer.validarTextoSeguro(dto.cor(), "cor");

        VeiculoJpaEntity veiculo = new VeiculoJpaEntity();
        veiculo.setUsuarioId(getLoggedUserId(auth));
        veiculo.setPlaca(SecuritySanitizer.sanitizar(dto.placa()).toUpperCase());
        veiculo.setModelo(SecuritySanitizer.sanitizar(dto.modelo()));
        veiculo.setCor(SecuritySanitizer.sanitizar(dto.cor()));
        return ResponseEntity.ok(veiculoRepository.save(veiculo));
    }

    @DeleteMapping("/veiculos/{id}")
    public ResponseEntity<Void> deleteVeiculo(Authentication auth, @PathVariable UUID id, HttpServletRequest request) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        return veiculoRepository.findByIdAndUsuarioId(id, user.getId())
                .map(veiculo -> {
                    veiculoRepository.delete(veiculo);
                    auditService.registrarUsuario(user, "EXCLUSAO_LOGICA_VEICULO", "INFO", request.getRemoteAddr(),
                            "Veículo placa '" + veiculo.getPlaca() + "' arquivado via Soft Delete (LGPD).");
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- PETS ---
    @GetMapping("/pets")
    public ResponseEntity<List<PetJpaEntity>> listarPets(Authentication auth) {
        return ResponseEntity.ok(petRepository.findByUsuarioId(getLoggedUserId(auth)));
    }

    @PostMapping("/pets")
    public ResponseEntity<PetJpaEntity> addPet(Authentication auth, @RequestBody @Valid AdicionarPetRequestDTO dto) {
        SecuritySanitizer.validarTextoSeguro(dto.nome(), "nome");
        SecuritySanitizer.validarTextoSeguro(dto.especie(), "especie");
        SecuritySanitizer.validarTextoSeguro(dto.raca(), "raca");

        PetJpaEntity pet = new PetJpaEntity();
        pet.setUsuarioId(getLoggedUserId(auth));
        pet.setNome(SecuritySanitizer.sanitizar(dto.nome()));
        pet.setEspecie(SecuritySanitizer.sanitizar(dto.especie()));
        pet.setRaca(dto.raca() != null ? SecuritySanitizer.sanitizar(dto.raca()) : null);
        return ResponseEntity.ok(petRepository.save(pet));
    }

    @DeleteMapping("/pets/{id}")
    public ResponseEntity<Void> deletePet(Authentication auth, @PathVariable UUID id, HttpServletRequest request) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        return petRepository.findByIdAndUsuarioId(id, user.getId())
                .map(pet -> {
                    petRepository.delete(pet);
                    auditService.registrarUsuario(user, "EXCLUSAO_LOGICA_PET", "INFO", request.getRemoteAddr(),
                            "Pet '" + pet.getNome() + "' arquivado via Soft Delete (LGPD).");
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- MORADORES ADICIONAIS ---
    @GetMapping("/moradores")
    public ResponseEntity<List<MoradorAdicionalJpaEntity>> listarMoradores(Authentication auth) {
        return ResponseEntity.ok(moradorAdicionalRepository.findByUsuarioId(getLoggedUserId(auth)));
    }

    @PostMapping("/moradores")
    public ResponseEntity<MoradorAdicionalJpaEntity> addMorador(Authentication auth, @RequestBody @Valid AdicionarMoradorRequestDTO dto) {
        SecuritySanitizer.validarTextoSeguro(dto.nome(), "nome");
        SecuritySanitizer.validarTextoSeguro(dto.parentesco(), "parentesco");

        MoradorAdicionalJpaEntity morador = new MoradorAdicionalJpaEntity();
        morador.setUsuarioId(getLoggedUserId(auth));
        morador.setNome(SecuritySanitizer.sanitizar(dto.nome()));
        morador.setParentesco(SecuritySanitizer.sanitizar(dto.parentesco()));
        return ResponseEntity.ok(moradorAdicionalRepository.save(morador));
    }

    @DeleteMapping("/moradores/{id}")
    public ResponseEntity<Void> deleteMorador(Authentication auth, @PathVariable UUID id, HttpServletRequest request) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) auth.getPrincipal();
        return moradorAdicionalRepository.findByIdAndUsuarioId(id, user.getId())
                .map(morador -> {
                    moradorAdicionalRepository.delete(morador);
                    auditService.registrarUsuario(user, "EXCLUSAO_LOGICA_MORADOR_ADICIONAL", "INFO", request.getRemoteAddr(),
                            "Morador dependente '" + morador.getNome() + "' arquivado via Soft Delete (LGPD).");
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

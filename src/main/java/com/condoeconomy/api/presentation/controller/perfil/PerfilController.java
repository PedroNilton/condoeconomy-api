package com.condoeconomy.api.presentation.controller.perfil;

import com.condoeconomy.api.infrastructure.persistence.entity.MoradorAdicionalJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.PetJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.VeiculoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataMoradorAdicionalRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPetRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataVeiculoRepository;
import com.condoeconomy.api.presentation.dto.perfil.UsuarioResponseDTO;
import com.condoeconomy.api.presentation.dto.perfil.AdicionarVeiculoRequestDTO;
import com.condoeconomy.api.presentation.dto.perfil.AdicionarPetRequestDTO;
import com.condoeconomy.api.presentation.dto.perfil.AdicionarMoradorRequestDTO;
import lombok.RequiredArgsConstructor;
import com.condoeconomy.api.presentation.dto.perfil.AtualizarDadosPessoaisRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.core.security.TokenService;
import com.condoeconomy.api.presentation.dto.perfil.AlterarSenhaRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final SpringDataVeiculoRepository veiculoRepository;
    private final SpringDataPetRepository petRepository;
    private final SpringDataMoradorAdicionalRepository moradorAdicionalRepository;
    private final SpringDataUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    private UUID getLoggedUserId(Authentication authentication) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) authentication.getPrincipal();
        return user.getId();
    }

    // --- SEGURANÇA / STEP-UP AUTH & REVOGAÇÃO DE SESSÕES ---

    @PutMapping("/seguranca/alterar-senha")
    public ResponseEntity<?> alterarSenha(Authentication auth, @RequestBody @Valid AlterarSenhaRequestDTO dto) {
        return usuarioRepository.findById(getLoggedUserId(auth))
                .map(usuario -> {
                    // 1. Step-up Auth: validação estrita da senha atual
                    if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
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

                    // 6. Emissão de novo token para manter a sessão do dispositivo atual
                    String novoToken = tokenService.generateToken(usuario);

                    return ResponseEntity.ok(Map.of(
                            "message", "Senha alterada com sucesso! Todas as outras sessões foram desconectadas por segurança.",
                            "token", novoToken
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/seguranca/encerrar-sessoes")
    public ResponseEntity<?> encerrarOutrasSessoes(Authentication auth) {
        return usuarioRepository.findById(getLoggedUserId(auth))
                .map(usuario -> {
                    // Incrementa tokenVersion, invalidando instantaneamente qualquer JWT emitido anteriormente
                    usuario.setTokenVersion(usuario.getTokenVersion() + 1);
                    usuarioRepository.save(usuario);

                    // Emite um novo token válido exclusivamente para a sessão ativa
                    String novoToken = tokenService.generateToken(usuario);

                    return ResponseEntity.ok(Map.of(
                            "message", "Todas as outras sessões foram encerradas com sucesso.",
                            "token", novoToken
                    ));
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
    public ResponseEntity<UsuarioResponseDTO> updateDadosPessoais(Authentication auth, @RequestBody @Valid AtualizarDadosPessoaisRequestDTO dados) {
        return usuarioRepository.findById(getLoggedUserId(auth))
                .map(usuario -> {
                    usuario.setNome(dados.nome());
                    usuario.setTelefone(dados.telefone());
                    // Estes campos vêm do frontend, mas o ideal em condomínio é não deixar o morador mudar o próprio apto/bloco sem aprovação.
                    // Mas mantendo compatibilidade com o frontend atual que não altera apto/bloco
                    // usuario.setApartamento(dados.apartamento());
                    // usuario.setBloco(dados.bloco());
                    usuario.setFoto(dados.foto());
                    UsuarioJpaEntity saved = usuarioRepository.save(usuario);
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
        VeiculoJpaEntity veiculo = new VeiculoJpaEntity();
        veiculo.setUsuarioId(getLoggedUserId(auth));
        veiculo.setPlaca(dto.placa());
        veiculo.setModelo(dto.modelo());
        veiculo.setCor(dto.cor());
        return ResponseEntity.ok(veiculoRepository.save(veiculo));
    }

    @DeleteMapping("/veiculos/{id}")
    public ResponseEntity<Void> deleteVeiculo(Authentication auth, @PathVariable UUID id) {
        return veiculoRepository.findByIdAndUsuarioId(id, getLoggedUserId(auth))
                .map(veiculo -> {
                    veiculoRepository.delete(veiculo);
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
        PetJpaEntity pet = new PetJpaEntity();
        pet.setUsuarioId(getLoggedUserId(auth));
        pet.setNome(dto.nome());
        pet.setEspecie(dto.especie());
        pet.setRaca(dto.raca());
        return ResponseEntity.ok(petRepository.save(pet));
    }

    @DeleteMapping("/pets/{id}")
    public ResponseEntity<Void> deletePet(Authentication auth, @PathVariable UUID id) {
        return petRepository.findByIdAndUsuarioId(id, getLoggedUserId(auth))
                .map(pet -> {
                    petRepository.delete(pet);
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
        MoradorAdicionalJpaEntity morador = new MoradorAdicionalJpaEntity();
        morador.setUsuarioId(getLoggedUserId(auth));
        morador.setNome(dto.nome());
        morador.setParentesco(dto.parentesco());
        return ResponseEntity.ok(moradorAdicionalRepository.save(morador));
    }

    @DeleteMapping("/moradores/{id}")
    public ResponseEntity<Void> deleteMorador(Authentication auth, @PathVariable UUID id) {
        return moradorAdicionalRepository.findByIdAndUsuarioId(id, getLoggedUserId(auth))
                .map(morador -> {
                    moradorAdicionalRepository.delete(morador);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

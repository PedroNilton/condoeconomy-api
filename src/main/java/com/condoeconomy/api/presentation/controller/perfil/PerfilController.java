package com.condoeconomy.api.presentation.controller.perfil;

import com.condoeconomy.api.infrastructure.persistence.entity.MoradorAdicionalJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.PetJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.VeiculoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataMoradorAdicionalRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataPetRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataVeiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final SpringDataVeiculoRepository veiculoRepository;
    private final SpringDataPetRepository petRepository;
    private final SpringDataMoradorAdicionalRepository moradorAdicionalRepository;
    private final SpringDataUsuarioRepository usuarioRepository;

    private UUID getLoggedUserId(Authentication authentication) {
        UsuarioJpaEntity user = (UsuarioJpaEntity) authentication.getPrincipal();
        return user.getId();
    }

    // --- DADOS PESSOAIS ---
    @GetMapping("/dados")
    public ResponseEntity<UsuarioJpaEntity> getDadosPessoais(Authentication auth) {
        return usuarioRepository.findById(getLoggedUserId(auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/dados")
    public ResponseEntity<UsuarioJpaEntity> updateDadosPessoais(Authentication auth, @RequestBody UsuarioJpaEntity dados) {
        return usuarioRepository.findById(getLoggedUserId(auth))
                .map(usuario -> {
                    usuario.setNome(dados.getNome());
                    usuario.setTelefone(dados.getTelefone());
                    return ResponseEntity.ok(usuarioRepository.save(usuario));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- VEÍCULOS ---
    @GetMapping("/veiculos")
    public ResponseEntity<List<VeiculoJpaEntity>> listarVeiculos(Authentication auth) {
        return ResponseEntity.ok(veiculoRepository.findByUsuarioId(getLoggedUserId(auth)));
    }

    @PostMapping("/veiculos")
    public ResponseEntity<VeiculoJpaEntity> addVeiculo(Authentication auth, @RequestBody VeiculoJpaEntity veiculo) {
        veiculo.setUsuarioId(getLoggedUserId(auth));
        return ResponseEntity.ok(veiculoRepository.save(veiculo));
    }

    @DeleteMapping("/veiculos/{id}")
    public ResponseEntity<Void> deleteVeiculo(Authentication auth, @PathVariable UUID id) {
        veiculoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- PETS ---
    @GetMapping("/pets")
    public ResponseEntity<List<PetJpaEntity>> listarPets(Authentication auth) {
        return ResponseEntity.ok(petRepository.findByUsuarioId(getLoggedUserId(auth)));
    }

    @PostMapping("/pets")
    public ResponseEntity<PetJpaEntity> addPet(Authentication auth, @RequestBody PetJpaEntity pet) {
        pet.setUsuarioId(getLoggedUserId(auth));
        return ResponseEntity.ok(petRepository.save(pet));
    }

    @DeleteMapping("/pets/{id}")
    public ResponseEntity<Void> deletePet(Authentication auth, @PathVariable UUID id) {
        petRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- MORADORES ADICIONAIS ---
    @GetMapping("/moradores")
    public ResponseEntity<List<MoradorAdicionalJpaEntity>> listarMoradores(Authentication auth) {
        return ResponseEntity.ok(moradorAdicionalRepository.findByUsuarioId(getLoggedUserId(auth)));
    }

    @PostMapping("/moradores")
    public ResponseEntity<MoradorAdicionalJpaEntity> addMorador(Authentication auth, @RequestBody MoradorAdicionalJpaEntity morador) {
        morador.setUsuarioId(getLoggedUserId(auth));
        return ResponseEntity.ok(moradorAdicionalRepository.save(morador));
    }

    @DeleteMapping("/moradores/{id}")
    public ResponseEntity<Void> deleteMorador(Authentication auth, @PathVariable UUID id) {
        moradorAdicionalRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

package com.condoeconomy.api.presentation.controller.portaria;

import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.entity.VeiculoJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataVeiculoRepository;
import com.condoeconomy.api.presentation.controller.portaria.dto.VeiculoConsultaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/portaria/veiculos")
@PreAuthorize("hasAnyRole('PORTEIRO', 'ADMIN', 'SINDICO')")
public class PortariaVeiculoController {

    @Autowired
    private SpringDataVeiculoRepository veiculoRepository;

    @Autowired
    private SpringDataUsuarioRepository usuarioRepository;

    @GetMapping("/consulta")
    public ResponseEntity<List<VeiculoConsultaDTO>> consultarVeiculo(@RequestParam String placa) {
        // Permite buscar digitando a placa com ou sem traço
        String busca = placa;
        if (placa.length() == 7 && !placa.contains("-")) {
            busca = placa.substring(0, 3) + "-" + placa.substring(3);
        }
        
        List<VeiculoJpaEntity> veiculos = veiculoRepository.findByPlacaContainingIgnoreCase(busca);

        List<VeiculoConsultaDTO> dtos = veiculos.stream().map(v -> {
            Optional<UsuarioJpaEntity> morador = usuarioRepository.findById(v.getUsuarioId());
            
            return VeiculoConsultaDTO.builder()
                    .id(v.getId())
                    .placa(v.getPlaca())
                    .modelo(v.getModelo())
                    .cor(v.getCor())
                    .moradorId(v.getUsuarioId())
                    .nomeMorador(morador.map(UsuarioJpaEntity::getNome).orElse("Desconhecido"))
                    .apartamento(morador.map(UsuarioJpaEntity::getApartamento).orElse(""))
                    .bloco(morador.map(UsuarioJpaEntity::getBloco).orElse(""))
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}

package com.condoeconomy.api.presentation.controller.reserva;

import com.condoeconomy.api.application.usecase.reserva.AprovarRejeitarReservaUseCase;
import com.condoeconomy.api.application.usecase.reserva.CheckInConvidadoUseCase;
import com.condoeconomy.api.application.usecase.reserva.CriarReservaUseCase;
import com.condoeconomy.api.application.usecase.reserva.ListarAreasComunsUseCase;
import com.condoeconomy.api.application.usecase.reserva.ListarReservasUseCase;
import com.condoeconomy.api.application.usecase.reserva.ListarMinhasReservasUseCase;
import com.condoeconomy.api.presentation.dto.reserva.AreaComumResponseDTO;
import com.condoeconomy.api.presentation.dto.reserva.CriarReservaRequestDTO;
import com.condoeconomy.api.presentation.dto.reserva.ReservaResponseDTO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final CriarReservaUseCase criarReservaUseCase;
    private final ListarReservasUseCase listarReservasUseCase;
    private final ListarAreasComunsUseCase listarAreasComunsUseCase;
    private final CheckInConvidadoUseCase checkInConvidadoUseCase;
    private final AprovarRejeitarReservaUseCase aprovarRejeitarReservaUseCase;
    private final ListarMinhasReservasUseCase listarMinhasReservasUseCase;

    public ReservaController(CriarReservaUseCase criarReservaUseCase, 
                             ListarReservasUseCase listarReservasUseCase, 
                             ListarAreasComunsUseCase listarAreasComunsUseCase,
                             CheckInConvidadoUseCase checkInConvidadoUseCase,
                             AprovarRejeitarReservaUseCase aprovarRejeitarReservaUseCase,
                             ListarMinhasReservasUseCase listarMinhasReservasUseCase) {
        this.criarReservaUseCase = criarReservaUseCase;
        this.listarReservasUseCase = listarReservasUseCase;
        this.listarAreasComunsUseCase = listarAreasComunsUseCase;
        this.checkInConvidadoUseCase = checkInConvidadoUseCase;
        this.aprovarRejeitarReservaUseCase = aprovarRejeitarReservaUseCase;
        this.listarMinhasReservasUseCase = listarMinhasReservasUseCase;
    }

    @GetMapping("/minhas")
    public ResponseEntity<List<ReservaResponseDTO>> listarMinhasReservas(org.springframework.security.core.Authentication auth) {
        try {
            com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity user = 
                (com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity) auth.getPrincipal();
            String unidadeConsulta = "Apto " + user.getApartamento() + " - Bloco " + user.getBloco();
            
            var reservas = listarMinhasReservasUseCase.executar(unidadeConsulta);
            var dtos = reservas.stream().map(ReservaResponseDTO::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/areas-comuns")
    public ResponseEntity<List<AreaComumResponseDTO>> listarAreasComuns() {
        var areas = listarAreasComunsUseCase.executar();
        var dtos = areas.stream().map(AreaComumResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping
    public ResponseEntity<List<ReservaResponseDTO>> listarReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String status) {
        
        List<com.condoeconomy.api.domain.entity.reserva.Reserva> reservas;
        if (data == null && status == null) {
            reservas = listarReservasUseCase.executar(LocalDate.now(), null);
        } else {
            reservas = listarReservasUseCase.executar(data, status);
        }

        var dtos = reservas.stream().map(ReservaResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> criarReserva(
            @Valid @RequestBody CriarReservaRequestDTO request, 
            org.springframework.security.core.Authentication auth) {
        
        com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity user = 
            (com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity) auth.getPrincipal();
        
        String unidade = "Apto " + user.getApartamento() + " - Bloco " + user.getBloco();
        String morador = user.getNome();

        List<CriarReservaUseCase.ConvidadoDtoInput> convidadosInput = request.convidados() != null 
                ? request.convidados().stream()
                    .map(c -> new CriarReservaUseCase.ConvidadoDtoInput(c.nome(), c.documento()))
                    .collect(Collectors.toList())
                : List.of();

        var reserva = criarReservaUseCase.executar(
                request.areaComumId(), unidade, morador, request.titulo(),
                request.data(), request.inicio(), request.fim(), convidadosInput
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponseDTO.fromEntity(reserva));
    }

    @PutMapping("/{id}/convidados/{convidadoId}/checkin")
    public ResponseEntity<ReservaResponseDTO> checkInConvidado(
            @PathVariable UUID id, @PathVariable UUID convidadoId) {
        var reserva = checkInConvidadoUseCase.executar(id, convidadoId);
        return ResponseEntity.ok(ReservaResponseDTO.fromEntity(reserva));
    }

    @PutMapping("/{id}/aprovar")
    public ResponseEntity<Void> aprovar(@PathVariable UUID id) {
        aprovarRejeitarReservaUseCase.aprovar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/rejeitar")
    public ResponseEntity<Void> rejeitar(@PathVariable UUID id) {
        aprovarRejeitarReservaUseCase.rejeitar(id);
        return ResponseEntity.noContent().build();
    }
}

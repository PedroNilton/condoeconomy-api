package com.condoeconomy.api.presentation.controller.reserva;

import com.condoeconomy.api.application.usecase.reserva.AprovarRejeitarReservaUseCase;
import com.condoeconomy.api.application.usecase.reserva.CheckInConvidadoUseCase;
import com.condoeconomy.api.application.usecase.reserva.CriarReservaUseCase;
import com.condoeconomy.api.application.usecase.reserva.ListarAreasComunsUseCase;
import com.condoeconomy.api.application.usecase.reserva.ListarReservasUseCase;
import com.condoeconomy.api.application.usecase.reserva.ListarMinhasReservasUseCase;
import com.condoeconomy.api.core.security.SecuritySanitizer;
import com.condoeconomy.api.presentation.dto.reserva.AreaComumResponseDTO;
import com.condoeconomy.api.presentation.dto.reserva.CriarReservaRequestDTO;
import com.condoeconomy.api.presentation.dto.reserva.ReservaResponseDTO;
import com.condoeconomy.api.presentation.dto.reserva.RejeitarReservaRequestDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private static final Logger log = LoggerFactory.getLogger(ReservaController.class);

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
    public ResponseEntity<List<ReservaResponseDTO>> listarMinhasReservas(Authentication auth) {
        com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity user = 
            (com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity) auth.getPrincipal();
        String unidadeConsulta = "Apto " + user.getApartamento() + " - Bloco " + user.getBloco();
        
        var reservas = listarMinhasReservasUseCase.executar(unidadeConsulta);
        var dtos = reservas.stream().map(ReservaResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/areas-comuns")
    public ResponseEntity<List<AreaComumResponseDTO>> listarAreasComuns() {
        var areas = listarAreasComunsUseCase.executar();
        var dtos = areas.stream().map(AreaComumResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<ReservaResponseDTO>> listarReservas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            SecuritySanitizer.validarTextoSeguro(status, "status");
        }
        
        List<com.condoeconomy.api.domain.entity.reserva.Reserva> reservas;
        if (data == null && status == null) {
            reservas = listarReservasUseCase.executar(LocalDate.now(), null);
        } else {
            reservas = listarReservasUseCase.executar(data, status != null ? SecuritySanitizer.sanitizar(status) : null);
        }

        var dtos = reservas.stream().map(ReservaResponseDTO::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<ReservaResponseDTO> criarReserva(
            @Valid @RequestBody CriarReservaRequestDTO request, 
            Authentication auth) {
        SecuritySanitizer.validarTextoSeguro(request.titulo(), "titulo");

        com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity user = 
            (com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity) auth.getPrincipal();
        
        String unidade = "Apto " + user.getApartamento() + " - Bloco " + user.getBloco();
        String morador = user.getNome();

        List<CriarReservaUseCase.ConvidadoDtoInput> convidadosInput = request.convidados() != null 
                ? request.convidados().stream()
                    .map(c -> {
                        SecuritySanitizer.validarTextoSeguro(c.nome(), "nomeConvidado");
                        SecuritySanitizer.validarTextoSeguro(c.documento(), "documentoConvidado");
                        return new CriarReservaUseCase.ConvidadoDtoInput(
                                SecuritySanitizer.sanitizar(c.nome()),
                                SecuritySanitizer.sanitizar(c.documento())
                        );
                    })
                    .collect(Collectors.toList())
                : List.of();

        var reserva = criarReservaUseCase.executar(
                request.areaComumId(),
                unidade,
                morador,
                SecuritySanitizer.sanitizar(request.titulo()),
                request.data(),
                request.inicio(),
                request.fim(),
                convidadosInput
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponseDTO.fromEntity(reserva));
    }

    @PutMapping("/{id}/convidados/{convidadoId}/checkin")
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ReservaResponseDTO> checkInConvidado(
            @PathVariable UUID id, @PathVariable UUID convidadoId, Authentication auth) {
        var reserva = checkInConvidadoUseCase.executar(id, convidadoId);
        String operador = auth != null ? auth.getName() : "SISTEMA";
        log.info("[AUDITORIA DE RESERVA] Check-in do convidado ID={} na reserva ID={} registrado pelo operador '{}'",
                convidadoId, id, operador);
        return ResponseEntity.ok(ReservaResponseDTO.fromEntity(reserva));
    }

    @PutMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> aprovar(@PathVariable UUID id, Authentication auth) {
        aprovarRejeitarReservaUseCase.aprovar(id);
        String operador = auth != null ? auth.getName() : "SISTEMA";
        log.info("[AUDITORIA DE RESERVA] Reserva ID={} aprovada pelo operador '{}'", id, operador);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/rejeitar")
    @PreAuthorize("hasAnyRole('ROLE_PORTEIRO', 'ROLE_SINDICO', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> rejeitar(@PathVariable UUID id, @RequestBody(required = false) @Valid RejeitarReservaRequestDTO dto, Authentication auth) {
        String motivo = null;
        if (dto != null && dto.motivo() != null && !dto.motivo().isBlank()) {
            SecuritySanitizer.validarTextoSeguro(dto.motivo(), "motivo");
            motivo = SecuritySanitizer.sanitizar(dto.motivo());
        }
        aprovarRejeitarReservaUseCase.rejeitar(id, motivo);
        String operador = auth != null ? auth.getName() : "SISTEMA";
        log.info("[AUDITORIA DE RESERVA] Reserva ID={} rejeitada pelo operador '{}' (motivo: '{}')",
                id, operador, motivo != null ? motivo : "Não informado");
        return ResponseEntity.noContent().build();
    }
}

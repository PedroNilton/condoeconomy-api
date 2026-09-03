package com.condoeconomy.api.presentation.controller.aviso;

import com.condoeconomy.api.application.usecase.aviso.CriarAvisoUseCase;
import com.condoeconomy.api.application.usecase.aviso.ListarAvisosUseCase;
import com.condoeconomy.api.presentation.dto.aviso.CriarAvisoRequestDTO;
import com.condoeconomy.api.presentation.dto.aviso.AvisoResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/avisos")
public class AvisoController {

    private final CriarAvisoUseCase criarAvisoUseCase;
    private final ListarAvisosUseCase listarAvisosUseCase;

    public AvisoController(CriarAvisoUseCase criarAvisoUseCase, ListarAvisosUseCase listarAvisosUseCase) {
        this.criarAvisoUseCase = criarAvisoUseCase;
        this.listarAvisosUseCase = listarAvisosUseCase;
    }

    @GetMapping
    public ResponseEntity<List<AvisoResponseDTO>> listar() {
        return ResponseEntity.ok(listarAvisosUseCase.execute());
    }

    @PostMapping
    public ResponseEntity<AvisoResponseDTO> criar(@RequestBody @Valid CriarAvisoRequestDTO data) {
        return ResponseEntity.ok(criarAvisoUseCase.execute(data));
    }
}

package com.condoeconomy.api.application.usecase.aviso;

import com.condoeconomy.api.domain.entity.aviso.Aviso;
import com.condoeconomy.api.infrastructure.repository.aviso.AvisoRepository;
import com.condoeconomy.api.presentation.dto.aviso.CriarAvisoRequestDTO;
import com.condoeconomy.api.presentation.dto.aviso.AvisoResponseDTO;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CriarAvisoUseCase {
    private final AvisoRepository repository;

    public CriarAvisoUseCase(AvisoRepository repository) {
        this.repository = repository;
    }

    public AvisoResponseDTO execute(CriarAvisoRequestDTO data) {
        Aviso aviso = new Aviso(
            UUID.randomUUID().toString(),
            data.titulo(),
            data.mensagem(),
            LocalDateTime.now(),
            data.autor()
        );
        repository.save(aviso);
        return new AvisoResponseDTO(
            aviso.getId(),
            aviso.getTitulo(),
            aviso.getMensagem(),
            aviso.getDataCriacao(),
            aviso.getAutor()
        );
    }
}

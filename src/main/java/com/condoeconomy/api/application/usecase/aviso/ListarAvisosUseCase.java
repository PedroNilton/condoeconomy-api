package com.condoeconomy.api.application.usecase.aviso;

import com.condoeconomy.api.infrastructure.repository.aviso.AvisoRepository;
import com.condoeconomy.api.presentation.dto.aviso.AvisoResponseDTO;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ListarAvisosUseCase {
    private final AvisoRepository repository;

    public ListarAvisosUseCase(AvisoRepository repository) {
        this.repository = repository;
    }

    public List<AvisoResponseDTO> execute() {
        return repository.findAllByOrderByDataCriacaoDesc().stream()
            .map(a -> new AvisoResponseDTO(
                a.getId(),
                a.getTitulo(),
                a.getMensagem(),
                a.getDataCriacao(),
                a.getAutor()
            ))
            .collect(Collectors.toList());
    }
}

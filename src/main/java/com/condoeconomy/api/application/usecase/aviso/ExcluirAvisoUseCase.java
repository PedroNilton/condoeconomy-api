package com.condoeconomy.api.application.usecase.aviso;

import com.condoeconomy.api.infrastructure.repository.aviso.AvisoRepository;
import org.springframework.stereotype.Service;

@Service
public class ExcluirAvisoUseCase {
    private final AvisoRepository repository;

    public ExcluirAvisoUseCase(AvisoRepository repository) {
        this.repository = repository;
    }

    public void execute(String id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        }
    }
}

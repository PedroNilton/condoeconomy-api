package com.condoeconomy.api.application.usecase;

import com.condoeconomy.api.application.gateway.CondominioRepository;
import com.condoeconomy.api.domain.entity.Condominio;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class CadastrarCondominioUseCase {

    private final CondominioRepository repository;

    public CadastrarCondominioUseCase(CondominioRepository repository) {
        this.repository = repository;
    }

    public Condominio executar(String nome, String cnpj, String tenantIdFormatado) {
        // Valida unicidade
        if (repository.buscarPorCnpj(cnpj).isPresent()) {
            throw new IllegalArgumentException("Já existe um condomínio cadastrado com este CNPJ.");
        }
        
        if (repository.existePorTenantId(tenantIdFormatado)) {
            throw new IllegalArgumentException("Este Identificador de Tenant já está em uso por outro condomínio.");
        }

        Condominio novoCondominio = new Condominio(UUID.randomUUID(), nome, cnpj, tenantIdFormatado);
        
        // Na vida real, também acionaríamos um evento para rodar o Flyway e criar o schema dinamicamente aqui.
        
        return repository.salvar(novoCondominio);
    }
}

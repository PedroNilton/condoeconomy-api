package com.condoeconomy.api.application.gateway;

import com.condoeconomy.api.domain.entity.Condominio;
import java.util.Optional;

public interface CondominioRepository {
    Condominio salvar(Condominio condominio);
    Optional<Condominio> buscarPorCnpj(String cnpj);
    boolean existePorTenantId(String tenantId);
}

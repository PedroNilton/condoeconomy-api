package com.condoeconomy.api.application.gateway;

import java.util.UUID;

public interface NotificationGateway {
    void notificarMoradoresDaUnidade(UUID unidadeId, String titulo, String mensagem);
}

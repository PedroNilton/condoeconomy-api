-- V29: Tabela de preferências de notificações por usuário (Push e E-mail)
CREATE TABLE IF NOT EXISTS preferencia_notificacao_usuario (
    usuario_id UUID PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    encomendas_push BOOLEAN NOT NULL DEFAULT TRUE,
    boletos_push BOOLEAN NOT NULL DEFAULT TRUE,
    boletos_email BOOLEAN NOT NULL DEFAULT TRUE,
    reservas_push BOOLEAN NOT NULL DEFAULT TRUE,
    comunicados_push BOOLEAN NOT NULL DEFAULT TRUE,
    comunicados_email BOOLEAN NOT NULL DEFAULT TRUE,
    resumo_email BOOLEAN NOT NULL DEFAULT TRUE,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

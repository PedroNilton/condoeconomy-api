-- V26: Tabela de tokens temporários (hash SHA-256) para recuperação segura de senha
CREATE TABLE IF NOT EXISTS token_recuperacao_senha (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    ip_solicitante VARCHAR(64),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_token_rec_usuario ON token_recuperacao_senha(usuario_id);
CREATE INDEX IF NOT EXISTS idx_token_rec_hash ON token_recuperacao_senha(token_hash);

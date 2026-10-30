-- V27: Adiciona controle de status ativo/bloqueado na conta de usuário e sincroniza auditoria Envers
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE usuario_aud ADD COLUMN IF NOT EXISTS telefone VARCHAR(50);
ALTER TABLE usuario_aud ADD COLUMN IF NOT EXISTS token_version INT;
ALTER TABLE usuario_aud ADD COLUMN IF NOT EXISTS ativo BOOLEAN;

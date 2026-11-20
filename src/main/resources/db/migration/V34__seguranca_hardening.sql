-- V34: Hardening de Segurança (vínculo imutável usuario_id em chamado e contador anti-brute-force por OTP)
ALTER TABLE chamado ADD COLUMN IF NOT EXISTS usuario_id UUID;
CREATE INDEX IF NOT EXISTS idx_chamado_usuario_id ON chamado(usuario_id);

ALTER TABLE token_recuperacao_senha ADD COLUMN IF NOT EXISTS tentativas_falhas INT NOT NULL DEFAULT 0;

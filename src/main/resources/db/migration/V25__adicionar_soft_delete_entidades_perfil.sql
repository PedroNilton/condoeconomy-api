-- V25: Adiciona suporte a Exclusão Lógica (Soft Delete) para retenção forense e conformidade LGPD
ALTER TABLE veiculo ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE pet ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE morador_adicional ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_veiculo_deleted_at ON veiculo(deleted_at);
CREATE INDEX IF NOT EXISTS idx_pet_deleted_at ON pet(deleted_at);
CREATE INDEX IF NOT EXISTS idx_morador_adicional_deleted_at ON morador_adicional(deleted_at);

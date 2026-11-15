-- Campos complementares identificados na auditoria de integração (Morador Adicional)

ALTER TABLE morador_adicional
    ADD COLUMN IF NOT EXISTS email VARCHAR(120),
    ADD COLUMN IF NOT EXISTS acesso_app BOOLEAN NOT NULL DEFAULT FALSE;

-- Campos adicionais para alinhamento completo com o Design Spec (Fase 6)

ALTER TABLE notificacao
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(40) NOT NULL DEFAULT 'GERAL';

ALTER TABLE aviso
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(50) NOT NULL DEFAULT 'Aviso geral',
    ADD COLUMN IF NOT EXISTS blocos TEXT,
    ADD COLUMN IF NOT EXISTS fixado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS visualizacoes INTEGER NOT NULL DEFAULT 0;

ALTER TABLE pet
    ADD COLUMN IF NOT EXISTS cor VARCHAR(40),
    ADD COLUMN IF NOT EXISTS foto_url TEXT;

ALTER TABLE veiculo
    ADD COLUMN IF NOT EXISTS vaga VARCHAR(30);

ALTER TABLE chamado
    ADD COLUMN IF NOT EXISTS nota_portaria TEXT;

CREATE TABLE IF NOT EXISTS chamado_mensagem (
    id UUID PRIMARY KEY,
    chamado_id UUID NOT NULL REFERENCES chamado(id) ON DELETE CASCADE,
    autor_nome VARCHAR(120),
    autor_papel VARCHAR(20),
    texto TEXT NOT NULL,
    enviada_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chamado_mensagem_chamado_id
    ON chamado_mensagem (chamado_id, enviada_em);

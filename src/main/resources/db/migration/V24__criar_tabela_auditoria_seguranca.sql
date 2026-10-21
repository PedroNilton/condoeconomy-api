-- Sincroniza colunas auditadas pelo Hibernate Envers na tabela usuario_aud
ALTER TABLE usuario_aud ADD COLUMN IF NOT EXISTS telefone VARCHAR(30);
ALTER TABLE usuario_aud ADD COLUMN IF NOT EXISTS token_version INT DEFAULT 1;

-- Tabela imutável de Trilha de Auditoria Forense de Segurança (Defense-in-Depth & LGPD)
CREATE TABLE IF NOT EXISTS auditoria_seguranca (
    id UUID PRIMARY KEY,
    usuario_id UUID,
    usuario_email VARCHAR(255) NOT NULL,
    papel VARCHAR(50),
    evento VARCHAR(80) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    ip_origem VARCHAR(64),
    detalhes VARCHAR(500),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auditoria_seguranca_usuario_id ON auditoria_seguranca(usuario_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_seguranca_usuario_email ON auditoria_seguranca(usuario_email);
CREATE INDEX IF NOT EXISTS idx_auditoria_seguranca_criado_em ON auditoria_seguranca(criado_em DESC);

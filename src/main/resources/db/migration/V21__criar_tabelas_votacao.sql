CREATE TABLE votacao (
    id UUID PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    data_abertura TIMESTAMP NOT NULL,
    data_encerramento TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    autor_id UUID REFERENCES usuario(id)
);

CREATE TABLE opcao_votacao (
    id UUID PRIMARY KEY,
    votacao_id UUID NOT NULL REFERENCES votacao(id) ON DELETE CASCADE,
    titulo VARCHAR(100) NOT NULL
);

CREATE TABLE voto (
    id UUID PRIMARY KEY,
    votacao_id UUID NOT NULL REFERENCES votacao(id) ON DELETE CASCADE,
    opcao_id UUID NOT NULL REFERENCES opcao_votacao(id) ON DELETE CASCADE,
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    data_voto TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_voto_usuario_votacao UNIQUE (usuario_id, votacao_id)
);

CREATE TABLE unidade (
    id UUID PRIMARY KEY,
    bloco VARCHAR(50) NOT NULL,
    numero VARCHAR(50) NOT NULL
);

CREATE TABLE morador (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    telefone VARCHAR(50),
    unidade_id UUID NOT NULL,
    CONSTRAINT fk_morador_unidade FOREIGN KEY (unidade_id) REFERENCES unidade (id)
);

CREATE TABLE encomenda (
    id UUID PRIMARY KEY,
    codigo_rastreio VARCHAR(100),
    transportadora VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    data_recebimento TIMESTAMP NOT NULL,
    data_retirada TIMESTAMP,
    unidade_id UUID NOT NULL,
    CONSTRAINT fk_encomenda_unidade FOREIGN KEY (unidade_id) REFERENCES unidade (id)
);

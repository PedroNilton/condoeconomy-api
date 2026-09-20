-- Tabela de revisões do Envers (obrigatória)
CREATE SEQUENCE revinfo_seq START WITH 1 INCREMENT BY 50;
CREATE TABLE revinfo (
    rev INTEGER PRIMARY KEY,
    revtstmp BIGINT
);

-- Tabela de auditoria para Usuario
CREATE TABLE usuario_aud (
    id UUID,
    rev INTEGER REFERENCES revinfo (rev),
    revtype SMALLINT,
    nome VARCHAR(255),
    email VARCHAR(255),
    senha VARCHAR(255),
    papel VARCHAR(50),
    apartamento VARCHAR(20),
    bloco VARCHAR(20),
    foto TEXT,
    PRIMARY KEY (id, rev)
);

-- Tabela de auditoria para Visitante
CREATE TABLE visitante_aud (
    id UUID,
    rev INTEGER REFERENCES revinfo (rev),
    revtype SMALLINT,
    nome VARCHAR(100),
    sobrenome VARCHAR(100),
    documento VARCHAR(20),
    data_visita DATE,
    bloco_destino VARCHAR(20),
    unidade_destino VARCHAR(50),
    morador_responsavel VARCHAR(100),
    placa_veiculo VARCHAR(20),
    tipo VARCHAR(30),
    status VARCHAR(30),
    hora_entrada TIMESTAMP,
    hora_saida TIMESTAMP,
    PRIMARY KEY (id, rev)
);

-- Tabela de auditoria para Reserva
CREATE TABLE reserva_aud (
    id UUID,
    rev INTEGER REFERENCES revinfo (rev),
    revtype SMALLINT,
    area_comum_id UUID,
    unidade_texto VARCHAR(255),
    morador_solicitante VARCHAR(255),
    titulo VARCHAR(255),
    data_reserva DATE,
    hora_inicio TIME,
    hora_fim TIME,
    status VARCHAR(50),
    data_solicitacao TIMESTAMP,
    motivo_rejeicao VARCHAR(255),
    PRIMARY KEY (id, rev)
);

-- Tabela de auditoria para Boleto
CREATE TABLE boleto_aud (
    id UUID,
    rev INTEGER REFERENCES revinfo (rev),
    revtype SMALLINT,
    unidade_texto VARCHAR(255),
    morador_responsavel VARCHAR(255),
    competencia VARCHAR(7),
    valor DECIMAL(10,2),
    data_vencimento DATE,
    data_pagamento DATE,
    status VARCHAR(50),
    linha_digitavel VARCHAR(100),
    url_pdf VARCHAR(255),
    PRIMARY KEY (id, rev)
);

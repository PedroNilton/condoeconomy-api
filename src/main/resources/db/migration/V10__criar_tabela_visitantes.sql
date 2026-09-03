-- Criação da tabela de visitantes
CREATE TABLE visitante (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    documento VARCHAR(20) NOT NULL,
    data_visita DATE NOT NULL,
    unidade_destino VARCHAR(50) NOT NULL,
    morador_responsavel VARCHAR(100) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    hora_entrada TIMESTAMP,
    hora_saida TIMESTAMP
);

-- Dados iniciais para teste
INSERT INTO visitante (id, nome, documento, data_visita, unidade_destino, morador_responsavel, tipo, status)
VALUES 
    (gen_random_uuid(), 'Roberto Carlos', 'MG-12.345.678', CURRENT_DATE, 'Apto 101 - Bloco B', 'Carlos Silva', 'VISITANTE', 'AGUARDANDO'),
    (gen_random_uuid(), 'Maria Eduarda (Faxineira)', '111.222.333-44', CURRENT_DATE, 'Apto 101 - Bloco B', 'Carlos Silva', 'PRESTADOR_SERVICO', 'AGUARDANDO'),
    (gen_random_uuid(), 'Técnico da Claro', 'RG 44.555.666-7', CURRENT_DATE, 'Apto 205 - Bloco A', 'Juliana', 'PRESTADOR_SERVICO', 'NO_CONDOMINIO');

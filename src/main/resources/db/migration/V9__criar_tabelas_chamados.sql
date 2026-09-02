CREATE TABLE chamado (
    id UUID PRIMARY KEY,
    unidade_texto VARCHAR(255) NOT NULL,
    morador_solicitante VARCHAR(255) NOT NULL,
    categoria VARCHAR(50) NOT NULL, -- RECLAMACAO, SUGESTAO, MANUTENCAO, OUTROS
    assunto VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,
    status VARCHAR(50) NOT NULL, -- ABERTO, EM_ANDAMENTO, RESOLVIDO
    data_abertura TIMESTAMP NOT NULL,
    data_resolucao TIMESTAMP
);

-- Inserir alguns chamados fakes para visualização
INSERT INTO chamado (id, unidade_texto, morador_solicitante, categoria, assunto, descricao, status, data_abertura) 
VALUES 
('c1c2d3e4-0000-0000-0000-000000000001', 'Apto 101', 'Carlos Silva', 'MANUTENCAO', 'Lâmpada queimada', 'A lâmpada do corredor do 1º andar está queimada.', 'ABERTO', CURRENT_TIMESTAMP),
('c1c2d3e4-0000-0000-0000-000000000002', 'Apto 302', 'José Pereira', 'RECLAMACAO', 'Barulho excessivo', 'Vizinho do 303 fazendo muito barulho após as 22h.', 'EM_ANDAMENTO', CURRENT_TIMESTAMP - INTERVAL '1 day'),
('c1c2d3e4-0000-0000-0000-000000000003', 'Apto 204', 'Maria Oliveira', 'SUGESTAO', 'Pintura da quadra', 'Sugiro pintarmos as linhas da quadra poliesportiva.', 'RESOLVIDO', CURRENT_TIMESTAMP - INTERVAL '5 days');

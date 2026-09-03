CREATE TABLE aviso (
    id VARCHAR(36) PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    mensagem TEXT NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    autor VARCHAR(50) NOT NULL
);

-- Inserir alguns avisos de demonstração
INSERT INTO aviso (id, titulo, mensagem, data_criacao, autor) VALUES 
('11111111-1111-1111-1111-111111111111', 'Manutenção da Piscina', 'A piscina ficará interditada nesta quinta-feira para tratamento de choque na água. Agradecemos a compreensão.', '2026-09-02 10:00:00', 'Administração'),
('22222222-2222-2222-2222-222222222222', 'Limpeza da Garagem', 'A limpeza da garagem ocorrerá no próximo sábado. Por favor, retirem seus veículos entre 08h e 12h.', '2026-09-03 08:30:00', 'Síndico');

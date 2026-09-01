CREATE TABLE boleto (
    id UUID PRIMARY KEY,
    unidade_texto VARCHAR(255) NOT NULL,
    morador_responsavel VARCHAR(255) NOT NULL,
    competencia VARCHAR(7) NOT NULL, -- Ex: 08/2026
    valor DECIMAL(10,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    status VARCHAR(50) NOT NULL, -- PENDENTE, PAGO, VENCIDO
    linha_digitavel VARCHAR(100),
    url_pdf VARCHAR(255)
);

-- Inserir alguns boletos fakes para facilitar a visualização no frontend
INSERT INTO boleto (id, unidade_texto, morador_responsavel, competencia, valor, data_vencimento, status, linha_digitavel) 
VALUES 
('b1c2d3e4-0000-0000-0000-000000000001', 'Apto 101', 'Carlos Silva', '08/2026', 650.00, CURRENT_DATE + INTERVAL '5 days', 'PENDENTE', '34191.09008 63396.902008 19485.492301 1 90000000065000'),
('b1c2d3e4-0000-0000-0000-000000000002', 'Apto 204', 'Maria Oliveira', '07/2026', 650.00, CURRENT_DATE - INTERVAL '15 days', 'VENCIDO', '34191.09008 63396.902008 19485.492301 1 90000000065000'),
('b1c2d3e4-0000-0000-0000-000000000003', 'Apto 302', 'José Pereira', '08/2026', 650.00, CURRENT_DATE - INTERVAL '2 days', 'PAGO', '34191.09008 63396.902008 19485.492301 1 90000000065000');

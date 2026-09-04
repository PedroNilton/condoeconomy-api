CREATE TABLE despesa (
    id UUID PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(10,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    data_pagamento DATE,
    status VARCHAR(50) NOT NULL -- PENDENTE, PAGO
);

-- Inserir despesas fakes para visualizar no frontend
INSERT INTO despesa (id, descricao, valor, data_vencimento, data_pagamento, status) 
VALUES 
('d1c2d3e4-0000-0000-0000-000000000001', 'Conta de Água', 4500.00, CURRENT_DATE - INTERVAL '1 days', CURRENT_DATE - INTERVAL '1 days', 'PAGO'),
('d1c2d3e4-0000-0000-0000-000000000002', 'Conta de Luz', 2300.00, CURRENT_DATE + INTERVAL '5 days', NULL, 'PENDENTE'),
('d1c2d3e4-0000-0000-0000-000000000003', 'Pagamento Funcionários', 15600.00, CURRENT_DATE + INTERVAL '10 days', NULL, 'PENDENTE');

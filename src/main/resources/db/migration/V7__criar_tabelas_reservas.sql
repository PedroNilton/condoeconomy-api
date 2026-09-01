CREATE TABLE area_comum (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    capacidade_maxima INT,
    taxa_locacao DECIMAL(10,2) DEFAULT 0.0
);

CREATE TABLE reserva (
    id UUID PRIMARY KEY,
    area_comum_id UUID NOT NULL,
    unidade_texto VARCHAR(255) NOT NULL,
    morador_solicitante VARCHAR(255) NOT NULL,
    titulo VARCHAR(255),
    data_reserva DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fim TIME NOT NULL,
    status VARCHAR(50) NOT NULL, -- APROVADA, CANCELADA, REALIZADA
    data_solicitacao TIMESTAMP NOT NULL,
    CONSTRAINT fk_reserva_area FOREIGN KEY (area_comum_id) REFERENCES area_comum (id)
);

CREATE TABLE convidado_reserva (
    id UUID PRIMARY KEY,
    reserva_id UUID NOT NULL,
    nome VARCHAR(255) NOT NULL,
    documento VARCHAR(50),
    status_entrada VARCHAR(50) NOT NULL, -- PENDENTE, ENTROU
    hora_entrada TIMESTAMP,
    CONSTRAINT fk_convidado_reserva FOREIGN KEY (reserva_id) REFERENCES reserva (id)
);

-- Inserir algumas áreas comuns padrões para facilitar os testes
INSERT INTO area_comum (id, nome, descricao, capacidade_maxima, taxa_locacao) 
VALUES 
('a1b2c3d4-0000-0000-0000-000000000001', 'Salão de Festas', 'Salão principal com cozinha e churrasqueira', 80, 150.00),
('a1b2c3d4-0000-0000-0000-000000000002', 'Churrasqueira 1', 'Churrasqueira perto da piscina', 20, 50.00),
('a1b2c3d4-0000-0000-0000-000000000003', 'Quadra Poliesportiva', 'Quadra de futsal, vôlei e basquete', 25, 0.00);

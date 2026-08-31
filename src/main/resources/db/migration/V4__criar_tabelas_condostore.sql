CREATE TABLE produto (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descricao TEXT,
    preco DECIMAL(10, 2) NOT NULL,
    quantidade_estoque INT NOT NULL
);

CREATE TABLE pedido (
    id UUID PRIMARY KEY,
    morador_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    CONSTRAINT fk_pedido_morador FOREIGN KEY (morador_id) REFERENCES morador (id)
);

CREATE TABLE item_pedido (
    id UUID PRIMARY KEY,
    pedido_id UUID NOT NULL,
    produto_id UUID NOT NULL,
    quantidade INT NOT NULL,
    preco_unitario DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_item_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_item_produto FOREIGN KEY (produto_id) REFERENCES produto (id)
);

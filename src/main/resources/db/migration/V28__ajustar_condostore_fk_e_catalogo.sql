-- V28: Corrige chave estrangeira de pedido para usuario(id) e popula catálogo inicial do CondoStore
ALTER TABLE pedido DROP CONSTRAINT IF EXISTS fk_pedido_morador;
ALTER TABLE pedido ADD CONSTRAINT fk_pedido_usuario FOREIGN KEY (morador_id) REFERENCES usuario (id) ON DELETE CASCADE;

INSERT INTO produto (id, nome, descricao, preco, quantidade_estoque)
SELECT 'a1000000-0000-0000-0000-000000000001'::uuid, 'Água Mineral Sem Gás 500ml', 'Garrafa gelada pronta para consumo no refrigerador 1.', 3.50, 40
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Água Mineral Sem Gás 500ml');

INSERT INTO produto (id, nome, descricao, preco, quantidade_estoque)
SELECT 'a1000000-0000-0000-0000-000000000002'::uuid, 'Refrigerante Zero Lata 350ml', 'Bebida gaseificada sem adição de açúcares.', 6.50, 30
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Refrigerante Zero Lata 350ml');

INSERT INTO produto (id, nome, descricao, preco, quantidade_estoque)
SELECT 'a1000000-0000-0000-0000-000000000003'::uuid, 'Café Torrado e Moído Gourmet 250g', ' Torra média, 100% arábica — gôndola mercearia.', 22.90, 18
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Café Torrado e Moído Gourmet 250g');

INSERT INTO produto (id, nome, descricao, preco, quantidade_estoque)
SELECT 'a1000000-0000-0000-0000-000000000004'::uuid, 'Pão de Forma Tradicional 450g', 'Macio e fresco, ideal para café da manhã.', 9.90, 15
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Pão de Forma Tradicional 450g');

INSERT INTO produto (id, nome, descricao, preco, quantidade_estoque)
SELECT 'a1000000-0000-0000-0000-000000000005'::uuid, 'Barra de Proteína Chocolate 45g', '15g de proteína por unidade, zero adição de açúcar.', 11.50, 25
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Barra de Proteína Chocolate 45g');

INSERT INTO produto (id, nome, descricao, preco, quantidade_estoque)
SELECT 'a1000000-0000-0000-0000-000000000006'::uuid, 'Sabão Líquido Concentrado 1L', 'Rende até 30 lavagens — seção conveniência doméstica.', 19.90, 12
WHERE NOT EXISTS (SELECT 1 FROM produto WHERE nome = 'Sabão Líquido Concentrado 1L');

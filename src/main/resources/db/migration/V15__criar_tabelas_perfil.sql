-- Adicionar telefone no usuario
ALTER TABLE usuario ADD COLUMN telefone VARCHAR(20);

-- Tabela Veiculos
CREATE TABLE veiculo (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    placa VARCHAR(10) NOT NULL,
    modelo VARCHAR(100) NOT NULL,
    cor VARCHAR(50) NOT NULL,
    CONSTRAINT fk_veiculo_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

-- Tabela Pets
CREATE TABLE pet (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,
    especie VARCHAR(50) NOT NULL,
    raca VARCHAR(100),
    CONSTRAINT fk_pet_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

-- Tabela Moradores Adicionais
CREATE TABLE morador_adicional (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL,
    nome VARCHAR(255) NOT NULL,
    parentesco VARCHAR(50) NOT NULL,
    CONSTRAINT fk_morador_adic_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

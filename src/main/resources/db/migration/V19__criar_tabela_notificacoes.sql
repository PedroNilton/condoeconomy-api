CREATE TABLE notificacao (
    id UUID PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    mensagem TEXT NOT NULL,
    lida BOOLEAN NOT NULL DEFAULT FALSE,
    data_criacao TIMESTAMP NOT NULL,
    usuario_id UUID NOT NULL,
    CONSTRAINT fk_notificacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

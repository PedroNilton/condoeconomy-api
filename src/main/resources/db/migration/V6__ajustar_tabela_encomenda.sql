ALTER TABLE encomenda ADD COLUMN destinatario VARCHAR(255);
ALTER TABLE encomenda ADD COLUMN unidade_texto VARCHAR(255);
ALTER TABLE encomenda ALTER COLUMN unidade_id DROP NOT NULL;

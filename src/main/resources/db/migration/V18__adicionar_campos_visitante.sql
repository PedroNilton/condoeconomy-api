ALTER TABLE visitante ADD COLUMN sobrenome VARCHAR(100);
ALTER TABLE visitante ADD COLUMN placa_veiculo VARCHAR(20);
ALTER TABLE visitante ADD COLUMN bloco_destino VARCHAR(20);
UPDATE visitante SET status = 'AGUARDANDO_LIBERACAO' WHERE status = 'AGUARDANDO';

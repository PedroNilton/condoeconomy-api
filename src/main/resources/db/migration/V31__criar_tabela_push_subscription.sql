-- V31: Cria a tabela push_subscription para notificações Web Push (VAPID)
CREATE TABLE IF NOT EXISTS push_subscription (
    id UUID PRIMARY KEY,
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    endpoint TEXT NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL
);

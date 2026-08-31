package com.condoeconomy.api.application.gateway;

import com.condoeconomy.api.domain.entity.Pedido;
import java.util.UUID;

public interface PedidoRepository {
    Pedido salvar(Pedido pedido);
}

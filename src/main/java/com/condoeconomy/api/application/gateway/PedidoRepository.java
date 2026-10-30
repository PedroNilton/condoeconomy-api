package com.condoeconomy.api.application.gateway;

import com.condoeconomy.api.domain.entity.Pedido;
import com.condoeconomy.api.presentation.dto.PedidoResponseDTO;

import java.util.List;
import java.util.UUID;

public interface PedidoRepository {
    Pedido salvar(Pedido pedido);
    List<PedidoResponseDTO> listarPorMorador(UUID moradorId);
}

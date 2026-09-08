package com.condoeconomy.api.application.service.payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface GatewayPagamentoService {
    
    /**
     * Registra o boleto no gateway de pagamento e retorna a URL ou linha digitável real.
     */
    String gerarBoletoReal(UUID boletoId, BigDecimal valor, String cpfCnpjPagador, String nomePagador);
    
}

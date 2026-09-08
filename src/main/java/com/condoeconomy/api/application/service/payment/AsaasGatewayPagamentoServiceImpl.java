package com.condoeconomy.api.application.service.payment;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AsaasGatewayPagamentoServiceImpl implements GatewayPagamentoService {

    @Override
    public String gerarBoletoReal(UUID boletoId, BigDecimal valor, String cpfCnpjPagador, String nomePagador) {
        // TODO: Fazer requisição HTTP POST para https://sandbox.asaas.com/api/v3/payments
        // usando a chave de API real e os dados do pagador.
        
        System.out.println("Integração Asaas simulada: Boleto de " + valor + " gerado para " + nomePagador);
        
        // Retornando uma linha digitável fake como se a API tivesse respondido
        return "34191.09008 63571.277308 71444.640008 3 955300000" + valor.toString().replace(".", "");
    }
}

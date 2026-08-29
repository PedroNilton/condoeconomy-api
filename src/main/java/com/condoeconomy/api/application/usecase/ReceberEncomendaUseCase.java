package com.condoeconomy.api.application.usecase;

import com.condoeconomy.api.application.gateway.EncomendaRepository;
import com.condoeconomy.api.application.gateway.NotificationGateway;
import com.condoeconomy.api.application.gateway.UnidadeRepository;
import com.condoeconomy.api.domain.entity.Encomenda;
import com.condoeconomy.api.domain.entity.Unidade;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReceberEncomendaUseCase {

    private final EncomendaRepository encomendaRepository;
    private final UnidadeRepository unidadeRepository;
    private final NotificationGateway notificationGateway;

    public ReceberEncomendaUseCase(EncomendaRepository encomendaRepository, 
                                   UnidadeRepository unidadeRepository, 
                                   NotificationGateway notificationGateway) {
        this.encomendaRepository = encomendaRepository;
        this.unidadeRepository = unidadeRepository;
        this.notificationGateway = notificationGateway;
    }

    public Encomenda executar(String codigoRastreio, String transportadora, UUID unidadeId) {
        // 1. Valida se a unidade existe no condomínio atual
        Unidade unidade = unidadeRepository.buscarPorId(unidadeId)
                .orElseThrow(() -> new IllegalArgumentException("Unidade não encontrada no condomínio."));

        // 2. Cria a encomenda de acordo com as regras de domínio (já nasce AGUARDANDO_RETIRADA)
        Encomenda novaEncomenda = new Encomenda(UUID.randomUUID(), codigoRastreio, transportadora, unidade.getId());

        // 3. Persiste no banco de dados (A infraestrutura fará isso)
        Encomenda encomendaSalva = encomendaRepository.salvar(novaEncomenda);

        // 4. Dispara a notificação Push para os moradores daquela unidade
        String titulo = "Nova encomenda chegou!";
        String mensagem = String.format("Um pacote da transportadora %s está aguardando retirada na portaria.", transportadora);
        notificationGateway.notificarMoradoresDaUnidade(unidadeId, titulo, mensagem);

        return encomendaSalva;
    }
}

package com.condoeconomy.api.application.usecase.chamado;

import com.condoeconomy.api.application.gateway.chamado.ChamadoRepository;
import com.condoeconomy.api.application.service.NotificationService;
import com.condoeconomy.api.domain.entity.chamado.Chamado;
import org.springframework.stereotype.Service;

@Service
public class CriarChamadoUseCase {
    private final ChamadoRepository chamadoRepository;
    private final NotificationService notificationService;

    public CriarChamadoUseCase(ChamadoRepository chamadoRepository, NotificationService notificationService) {
        this.chamadoRepository = chamadoRepository;
        this.notificationService = notificationService;
    }

    public Chamado executar(String unidadeTexto, String morador, String categoriaStr, String assunto, String descricao) {
        return executar(unidadeTexto, morador, categoriaStr, assunto, descricao, null);
    }

    public Chamado executar(String unidadeTexto, String morador, String categoriaStr, String assunto, String descricao, String fotoAnexo) {
        Chamado.CategoriaChamado categoria = Chamado.CategoriaChamado.valueOf(categoriaStr.toUpperCase());
        Chamado chamado = Chamado.abrir(unidadeTexto, morador, categoria, assunto, descricao, fotoAnexo);
        Chamado salvo = chamadoRepository.salvar(chamado);
        notificationService.notifyChamadosUpdate();
        return salvo;
    }
}

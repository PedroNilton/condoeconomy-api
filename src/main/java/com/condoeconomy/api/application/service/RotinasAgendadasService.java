package com.condoeconomy.api.application.service;

import com.condoeconomy.api.domain.enums.VotacaoStatus;
import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataBoletoRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.reserva.SpringDataReservaRepository;
import com.condoeconomy.api.infrastructure.repository.VotacaoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RotinasAgendadasService {

    private static final Logger log = LoggerFactory.getLogger(RotinasAgendadasService.class);

    private final SpringDataBoletoRepository boletoRepository;
    private final VotacaoRepository votacaoRepository;
    private final SpringDataReservaRepository reservaRepository;
    private final NotificationService notificationService;

    /**
     * Varrer boletos pendentes cuja data de vencimento expirou e alterar para VENCIDO.
     * Roda diariamente à 00:01 e a cada 1 hora.
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Scheduled(initialDelay = 15000, fixedRate = 3600000)
    @Transactional
    public int atualizarBoletosVencidos() {
        LocalDate hoje = LocalDate.now();
        var boletosAtrasados = boletoRepository.findByStatusAndDataVencimentoBefore("PENDENTE", hoje);
        if (boletosAtrasados.isEmpty()) {
            return 0;
        }

        for (var boleto : boletosAtrasados) {
            boleto.setStatus("VENCIDO");
            boletoRepository.save(boleto);
        }

        log.info("[ROTINA AGENDADA] {} boleto(s) pendente(s) com data anterior a {} atualizado(s) para 'VENCIDO'.",
                boletosAtrasados.size(), hoje);
        return boletosAtrasados.size();
    }

    /**
     * Encerra votações de assembleias cujo prazo final de deliberação expirou.
     * Roda a cada 5 minutos.
     */
    @Scheduled(initialDelay = 20000, fixedRate = 300000)
    @Transactional
    public int encerrarVotacoesExpiradas() {
        LocalDateTime agora = LocalDateTime.now();
        var votacoesExpiradas = votacaoRepository.findByStatusAndDataEncerramentoBefore(VotacaoStatus.ABERTA, agora);
        if (votacoesExpiradas.isEmpty()) {
            return 0;
        }

        for (var votacao : votacoesExpiradas) {
            votacao.setStatus(VotacaoStatus.ENCERRADA);
            votacaoRepository.save(votacao);
            log.info("[ROTINA AGENDADA] Assembleia/Votação '{}' (ID={}) encerrada automaticamente por decurso de prazo.",
                    votacao.getTitulo(), votacao.getId());
        }

        return votacoesExpiradas.size();
    }

    /**
     * Cancela reservas pendentes de aprovação cuja data do evento já passou sem manifestação do síndico.
     * Roda a cada 1 hora.
     */
    @Scheduled(initialDelay = 25000, fixedRate = 3600000)
    @Transactional
    public int expirarReservasNaoRespondidas() {
        LocalDate hoje = LocalDate.now();
        var reservasExpiradas = reservaRepository.findByStatusAndDataReservaBefore("PENDENTE_APROVACAO", hoje);
        if (reservasExpiradas.isEmpty()) {
            return 0;
        }

        for (var reserva : reservasExpiradas) {
            reserva.setStatus("REJEITADA");
            reserva.setMotivoRejeicao("Cancelada automaticamente: data do evento expirou sem deliberação da administração.");
            reservaRepository.save(reserva);
            log.info("[ROTINA AGENDADA] Reserva ID={} (Unidade '{}') expirada automaticamente.",
                    reserva.getId(), reserva.getUnidadeTexto());
        }

        notificationService.notifyReservasUpdate();
        return reservasExpiradas.size();
    }
}

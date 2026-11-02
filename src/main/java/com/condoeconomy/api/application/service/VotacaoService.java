package com.condoeconomy.api.application.service;

import com.condoeconomy.api.domain.entity.OpcaoVotacao;
import com.condoeconomy.api.domain.entity.Votacao;
import com.condoeconomy.api.domain.entity.Voto;
import com.condoeconomy.api.domain.enums.VotacaoStatus;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import com.condoeconomy.api.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.condoeconomy.api.infrastructure.persistence.repository.financeiro.SpringDataBoletoRepository;
import com.condoeconomy.api.infrastructure.repository.VotacaoRepository;
import com.condoeconomy.api.infrastructure.repository.VotoRepository;
import com.condoeconomy.api.presentation.dto.votacao.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VotacaoService {

    private final VotacaoRepository votacaoRepository;
    private final VotoRepository votoRepository;
    private final SpringDataUsuarioRepository usuarioRepository;
    private final SpringDataBoletoRepository boletoRepository;

    public boolean verificarAdimplencia(UsuarioJpaEntity usuario) {
        if (usuario == null) return false;
        if (usuario.getApartamento() == null || usuario.getApartamento().isBlank()) {
            return true;
        }

        String apto = usuario.getApartamento();
        LocalDate hoje = LocalDate.now();
        var boletos = boletoRepository.findByUnidadeTextoContainingIgnoreCaseOrderByDataVencimentoDesc(apto);
        
        return boletos.stream().noneMatch(b -> 
            "VENCIDO".equalsIgnoreCase(b.getStatus()) || 
            ("PENDENTE".equalsIgnoreCase(b.getStatus()) && b.getDataVencimento() != null && b.getDataVencimento().isBefore(hoje))
        );
    }

    @Transactional
    public VotacaoResponseDTO criarVotacao(VotacaoRequestDTO dto, String autorEmail) {
        UsuarioJpaEntity autor = usuarioRepository.findByEmail(autorEmail)
                .orElseThrow(() -> new EntityNotFoundException("Autor não encontrado"));

        Votacao votacao = new Votacao();
        votacao.setTitulo(dto.getTitulo());
        votacao.setDescricao(dto.getDescricao());
        votacao.setDataAbertura(LocalDateTime.now());
        votacao.setDataEncerramento(dto.getDataEncerramento());
        votacao.setStatus(VotacaoStatus.ABERTA);
        votacao.setAutor(autor);

        for (String tituloOpcao : dto.getOpcoes()) {
            OpcaoVotacao opcao = new OpcaoVotacao();
            opcao.setTitulo(tituloOpcao);
            votacao.addOpcao(opcao);
        }

        votacao = votacaoRepository.save(votacao);
        return mapToDTO(votacao, autor.getId(), true);
    }

    @Transactional(readOnly = true)
    public List<VotacaoResponseDTO> listarAtivas(String usuarioEmail) {
        UsuarioJpaEntity usuario = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        boolean adimplente = verificarAdimplencia(usuario);
        List<Votacao> ativas = votacaoRepository.findByStatusOrderByDataAberturaDesc(VotacaoStatus.ABERTA);
        return ativas.stream()
                .map(v -> {
                    VotacaoResponseDTO dto = mapToDTO(v, usuario.getId(), adimplente);
                    if (!adimplente) {
                        dto.setMotivoInaptidao("Conforme o Art. 1.335, III do Código Civil, apenas condôminos adimplentes podem votar nas assembleias.");
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VotacaoResponseDTO> listarEncerradas(String usuarioEmail) {
        UsuarioJpaEntity usuario = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        List<Votacao> encerradas = votacaoRepository.findByStatusOrderByDataAberturaDesc(VotacaoStatus.ENCERRADA);
        return encerradas.stream()
                .map(v -> mapToDTO(v, usuario.getId(), true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VotacaoResponseDTO> listarTodas(String usuarioEmail) {
        UsuarioJpaEntity usuario = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        List<Votacao> todas = votacaoRepository.findAll();
        todas.sort((a, b) -> b.getDataAbertura().compareTo(a.getDataAbertura())); // Descending
        return todas.stream()
                .map(v -> mapToDTO(v, usuario.getId(), true))
                .collect(Collectors.toList());
    }

    @Transactional
    public void registrarVoto(UUID votacaoId, VotoRequestDTO dto, String usuarioEmail) {
        UsuarioJpaEntity usuario = usuarioRepository.findByEmail(usuarioEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        Votacao votacao = votacaoRepository.findById(votacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Votação não encontrada"));

        if (votacao.getStatus() != VotacaoStatus.ABERTA || votacao.getDataEncerramento().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Esta votação já está encerrada.");
        }

        if (!verificarAdimplencia(usuario)) {
            throw new IllegalStateException("Conforme o Art. 1.335, III do Código Civil, apenas condôminos adimplentes podem votar nas assembleias. Regularize suas pendências financeiras na aba de Boletos.");
        }

        if (votoRepository.existsByVotacaoIdAndUsuarioId(votacaoId, usuario.getId())) {
            throw new IllegalStateException("Você já votou nesta assembleia.");
        }

        OpcaoVotacao opcao = votacao.getOpcoes().stream()
                .filter(o -> o.getId().equals(dto.getOpcaoId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Opção inválida para esta votação"));

        Voto voto = new Voto();
        voto.setVotacao(votacao);
        voto.setOpcao(opcao);
        voto.setUsuario(usuario);

        votoRepository.save(voto);
    }

    @Transactional
    public void encerrarVotacao(UUID votacaoId) {
        Votacao votacao = votacaoRepository.findById(votacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Votação não encontrada"));
        votacao.setStatus(VotacaoStatus.ENCERRADA);
        votacaoRepository.save(votacao);
    }

    @Transactional(readOnly = true)
    public VotacaoResultadoDTO obterResultado(UUID votacaoId) {
        Votacao votacao = votacaoRepository.findById(votacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Votação não encontrada"));

        List<Voto> votos = votoRepository.findByVotacaoId(votacaoId);
        int totalVotos = votos.size();

        VotacaoResultadoDTO resultado = new VotacaoResultadoDTO();
        resultado.setId(votacao.getId());
        resultado.setTitulo(votacao.getTitulo());
        resultado.setStatus(votacao.getStatus());
        resultado.setTotalVotos(totalVotos);

        List<OpcaoResultadoDTO> opcoesResultado = votacao.getOpcoes().stream().map(opcao -> {
            long votosOpcao = votos.stream().filter(v -> v.getOpcao().getId().equals(opcao.getId())).count();
            double percentual = totalVotos > 0 ? ((double) votosOpcao / totalVotos) * 100 : 0;
            return new OpcaoResultadoDTO(opcao.getId(), opcao.getTitulo(), (int) votosOpcao, percentual);
        }).collect(Collectors.toList());

        resultado.setOpcoes(opcoesResultado);
        return resultado;
    }

    private VotacaoResponseDTO mapToDTO(Votacao votacao, UUID usuarioId, boolean adimplente) {
        VotacaoResponseDTO dto = new VotacaoResponseDTO();
        dto.setId(votacao.getId());
        dto.setTitulo(votacao.getTitulo());
        dto.setDescricao(votacao.getDescricao());
        dto.setDataAbertura(votacao.getDataAbertura());
        dto.setDataEncerramento(votacao.getDataEncerramento());
        dto.setStatus(votacao.getStatus());
        dto.setAutorNome(votacao.getAutor().getNome());
        dto.setUsuarioAdimplente(adimplente);

        List<OpcaoVotacaoDTO> opcoesDTO = votacao.getOpcoes().stream().map(o -> {
            OpcaoVotacaoDTO odto = new OpcaoVotacaoDTO();
            odto.setId(o.getId());
            odto.setTitulo(o.getTitulo());
            return odto;
        }).collect(Collectors.toList());
        dto.setOpcoes(opcoesDTO);

        boolean jaVotou = votoRepository.existsByVotacaoIdAndUsuarioId(votacao.getId(), usuarioId);
        dto.setUsuarioJaVotou(jaVotou);

        return dto;
    }
}

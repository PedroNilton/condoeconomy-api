package com.condoeconomy.api.domain.entity;

import com.condoeconomy.api.domain.enums.VotacaoStatus;
import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "votacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Votacao {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String titulo;
    private String descricao;
    
    @Column(name = "data_abertura")
    private LocalDateTime dataAbertura;
    
    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;
    
    @Enumerated(EnumType.STRING)
    private VotacaoStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private UsuarioJpaEntity autor;

    @OneToMany(mappedBy = "votacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OpcaoVotacao> opcoes = new ArrayList<>();
    
    public void addOpcao(OpcaoVotacao opcao) {
        opcoes.add(opcao);
        opcao.setVotacao(this);
    }
}

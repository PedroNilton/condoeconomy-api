package com.condoeconomy.api.presentation.dto.perfil;

import com.condoeconomy.api.infrastructure.persistence.entity.UsuarioJpaEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UsuarioResponseDTO {
    private UUID id;
    private String nome;
    private String email;
    private String papel;
    private String telefone;
    private String bloco;
    private String apartamento;
    private String foto;

    public static UsuarioResponseDTO fromEntity(UsuarioJpaEntity entity) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setEmail(entity.getEmail());
        dto.setPapel(entity.getPapel());
        dto.setTelefone(entity.getTelefone());
        dto.setBloco(entity.getBloco());
        dto.setApartamento(entity.getApartamento());
        dto.setFoto(entity.getFoto());
        return dto;
    }
}

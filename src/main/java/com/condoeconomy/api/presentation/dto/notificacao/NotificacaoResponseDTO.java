package com.condoeconomy.api.presentation.dto.notificacao;
import java.time.LocalDateTime;
import java.util.UUID;
public record NotificacaoResponseDTO(UUID id, String titulo, String mensagem, boolean lida, LocalDateTime dataCriacao) {}

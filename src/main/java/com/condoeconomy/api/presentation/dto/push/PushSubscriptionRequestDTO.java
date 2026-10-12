package com.condoeconomy.api.presentation.dto.push;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushSubscriptionRequestDTO {
    @NotBlank(message = "O endpoint é obrigatório")
    @Size(max = 2048, message = "Endpoint excede o tamanho máximo permitido")
    private String endpoint;

    @NotNull(message = "As chaves de criptografia são obrigatórias")
    @Valid
    private Keys keys;

    @Getter
    @Setter
    public static class Keys {
        @NotBlank(message = "A chave p256dh é obrigatória")
        @Size(max = 255)
        private String p256dh;

        @NotBlank(message = "O segredo auth é obrigatório")
        @Size(max = 255)
        private String auth;
    }
}

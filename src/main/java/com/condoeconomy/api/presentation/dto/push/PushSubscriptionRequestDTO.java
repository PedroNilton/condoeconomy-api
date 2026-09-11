package com.condoeconomy.api.presentation.dto.push;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PushSubscriptionRequestDTO {
    private String endpoint;
    private Keys keys;

    @Getter
    @Setter
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}

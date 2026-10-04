package com.condoeconomy.api.core.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção simples contra força bruta no login.
 * <p>
 * Após {@value #MAX_TENTATIVAS} falhas consecutivas para o mesmo e-mail + IP, o acesso é bloqueado
 * por {@link #JANELA}. Implementação em memória (adequada para instância única).
 * Em ambiente com múltiplas instâncias, migrar para Redis/Bucket4j.
 */
@Service
public class LoginAttemptService {

    static final int MAX_TENTATIVAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);

    private record Tentativas(int falhas, Instant primeiraFalha) {}

    private final ConcurrentHashMap<String, Tentativas> cache = new ConcurrentHashMap<>();

    private String chave(String email, String ip) {
        return (email == null ? "" : email.trim().toLowerCase(Locale.ROOT)) + "|" + ip;
    }

    public boolean estaBloqueado(String email, String ip) {
        Tentativas t = cache.get(chave(email, ip));
        if (t == null) return false;
        if (Instant.now().isAfter(t.primeiraFalha().plus(JANELA))) {
            cache.remove(chave(email, ip));
            return false;
        }
        return t.falhas() >= MAX_TENTATIVAS;
    }

    public void registrarFalha(String email, String ip) {
        cache.compute(chave(email, ip), (k, atual) -> {
            Instant agora = Instant.now();
            if (atual == null || agora.isAfter(atual.primeiraFalha().plus(JANELA))) {
                return new Tentativas(1, agora);
            }
            return new Tentativas(atual.falhas() + 1, atual.primeiraFalha());
        });
    }

    public void registrarSucesso(String email, String ip) {
        cache.remove(chave(email, ip));
    }
}

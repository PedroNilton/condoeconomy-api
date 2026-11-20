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
    static final int MAX_TENTATIVAS_CONTA_GLOBAL = 10;
    static final int MAX_CACHE_ENTRIES = 10_000;
    static final Duration JANELA = Duration.ofMinutes(15);

    private record Tentativas(int falhas, Instant primeiraFalha) {}

    private final ConcurrentHashMap<String, Tentativas> cache = new ConcurrentHashMap<>();

    private String chave(String email, String ip) {
        return (email == null ? "" : email.trim().toLowerCase(Locale.ROOT)) + "|" + ip;
    }

    private String chaveConta(String email) {
        return "ACCOUNT|" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private void limparExpiradosSeCheio() {
        if (cache.size() < MAX_CACHE_ENTRIES) {
            return;
        }
        Instant agora = Instant.now();
        cache.entrySet().removeIf(entry -> agora.isAfter(entry.getValue().primeiraFalha().plus(JANELA)));
    }

    public boolean estaBloqueado(String email, String ip) {
        return verificarChaveBloqueada(chave(email, ip), MAX_TENTATIVAS)
                || verificarChaveBloqueada(chaveConta(email), MAX_TENTATIVAS_CONTA_GLOBAL);
    }

    private boolean verificarChaveBloqueada(String key, int limite) {
        Tentativas t = cache.get(key);
        if (t == null) return false;
        if (Instant.now().isAfter(t.primeiraFalha().plus(JANELA))) {
            cache.remove(key);
            return false;
        }
        return t.falhas() >= limite;
    }

    public void registrarFalha(String email, String ip) {
        limparExpiradosSeCheio();
        incrementarChave(chave(email, ip), JANELA);
        if (email != null && !email.isBlank()) {
            incrementarChave(chaveConta(email), JANELA);
        }
    }

    private Tentativas incrementarChave(String key, Duration janela) {
        return cache.compute(key, (k, atual) -> {
            Instant agora = Instant.now();
            if (atual == null || agora.isAfter(atual.primeiraFalha().plus(janela))) {
                return new Tentativas(1, agora);
            }
            return new Tentativas(atual.falhas() + 1, atual.primeiraFalha());
        });
    }

    public void registrarSucesso(String email, String ip) {
        cache.remove(chave(email, ip));
        if (email != null && !email.isBlank()) {
            cache.remove(chaveConta(email));
        }
    }

    /**
     * Proteção contra flood/abuso em endpoints públicos (ex: Auto Check-in na portaria via QR Code).
     * Limita a quantidade máxima de requisições por IP dentro de uma janela de 5 minutos.
     */
    public boolean verificarEContabilizarRequisicaoPublica(String ip, String recurso, int maxPorCincoMinutos) {
        limparExpiradosSeCheio();
        String key = "PUBLIC_RATE|" + recurso + "|" + (ip == null ? "unknown" : ip);
        Tentativas t = incrementarChave(key, Duration.ofMinutes(5));
        return t.falhas() <= maxPorCincoMinutos;
    }
}

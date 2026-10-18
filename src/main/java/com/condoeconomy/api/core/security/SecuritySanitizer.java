package com.condoeconomy.api.core.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Utilitário central de higienização e validação de segurança.
 * Protege contra injeções SQL, Cross-Site Scripting (XSS), Path Traversal,
 * Command Injection e injeção de bytes nulos/caracteres de controle.
 */
public final class SecuritySanitizer {

    private static final Logger log = LoggerFactory.getLogger(SecuritySanitizer.class);

    // Padrões de XSS e scripts maliciosos
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "(?i)<script|javascript:|vbscript:|onload\\s*=|onerror\\s*=|onclick\\s*=|<iframe|<object|<embed|<svg.*onload"
    );

    // Padrões de SQL Injection comuns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)('|--|/\\*|\\*/|;\\s*drop|;\\s*delete|;\\s*insert|;\\s*update|union\\s+select|or\\s+1=1|and\\s+1=1|exec\\s*\\()"
    );

    // Caracteres de controle e Null Bytes (ASCII 0-31 e 127)
    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]");

    private SecuritySanitizer() {}

    /**
     * Verifica se o texto contém assinaturas típicas de ataques de injeção ou script.
     */
    public static boolean contemPadroesMaliciosos(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        return SCRIPT_PATTERN.matcher(input).find()
                || SQL_INJECTION_PATTERN.matcher(input).find()
                || CONTROL_CHARS_PATTERN.matcher(input).find();
    }

    /**
     * Valida se a string é segura. Lança IllegalArgumentException caso encontre padrões de ataque.
     */
    public static void validarTextoSeguro(String input, String nomeCampo) {
        if (input == null || input.isBlank()) {
            return;
        }

        if (CONTROL_CHARS_PATTERN.matcher(input).find()) {
            log.warn("Tentativa de injeção de caracteres de controle/null byte detectada no campo '{}'", nomeCampo);
            throw new IllegalArgumentException("Caracteres de controle inválidos detectados no campo " + nomeCampo + ".");
        }

        if (SCRIPT_PATTERN.matcher(input).find() || SQL_INJECTION_PATTERN.matcher(input).find()) {
            log.warn("Tentativa de injeção de script/SQL detectada no campo '{}'", nomeCampo);
            throw new IllegalArgumentException("Conteúdo não permitido detectado no campo " + nomeCampo + ".");
        }
    }

    /**
     * Higieniza texto removendo caracteres de controle, null bytes, tags de script/HTML e espaços extras.
     */
    public static String sanitizar(String input) {
        if (input == null) {
            return null;
        }
        String limpo = CONTROL_CHARS_PATTERN.matcher(input).replaceAll("");
        limpo = limpo.replaceAll("<[^>]*>", "");
        return limpo.trim();
    }

    /**
     * Valida se uma URL de foto ou Data URI (Base64) é segura (apenas http/https ou data:image/jpeg|png|webp).
     * Bloqueia vetores XSS como javascript:, vbscript:, data:text/html ou data:image/svg+xml.
     */
    public static void validarUrlFotoSegura(String foto) {
        if (foto == null || foto.isBlank()) {
            return;
        }
        String trimmed = foto.trim().toLowerCase();
        boolean isDataImageSegura = trimmed.startsWith("data:image/jpeg;base64,")
                || trimmed.startsWith("data:image/jpg;base64,")
                || trimmed.startsWith("data:image/png;base64,")
                || trimmed.startsWith("data:image/webp;base64,");
        boolean isHttpUrlSegura = (trimmed.startsWith("https://") || trimmed.startsWith("http://"))
                && !contemPadroesMaliciosos(foto);

        if (!isDataImageSegura && !isHttpUrlSegura) {
            log.warn("Tentativa de envio de formato ou protocolo de imagem inseguro bloqueada.");
            throw new IllegalArgumentException("Formato ou origem de imagem não permitido por motivos de segurança.");
        }
    }

    /**
     * Mascara documentos pessoais (CPF/RG) em conformidade com o princípio de minimização de dados da LGPD.
     */
    public static String mascararDocumento(String doc) {
        if (doc == null || doc.isBlank()) {
            return null;
        }
        String limpo = doc.replaceAll("[^0-9a-zA-Z]", "");
        if (limpo.length() <= 4) {
            return "****";
        }
        if (limpo.length() == 11) {
            return "***." + limpo.substring(3, 6) + "." + limpo.substring(6, 9) + "-**";
        }
        int len = limpo.length();
        return "***." + limpo.substring(len - 4, len - 2) + "**";
    }
}

package com.condoeconomy.api.core.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
// IMPORTANTE: sem esta anotação, todos os @PreAuthorize dos controllers são ignorados silenciosamente.
@EnableMethodSecurity
public class SecurityConfig {

    /** Papéis da equipe do condomínio (operam portaria / administração). */
    private static final String[] STAFF = {"PORTEIRO", "SINDICO", "ADMIN", "SUPER_ADMIN"};
    private static final String[] STAFF_OU_MORADOR = {"MORADOR", "PORTEIRO", "SINDICO", "ADMIN", "SUPER_ADMIN"};
    /** Papéis com poder de gestão (financeiro, status de chamados). */
    private static final String[] GESTAO = {"SINDICO", "ADMIN", "SUPER_ADMIN"};

    private final SecurityFilter securityFilter;

    public SecurityConfig(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(Customizer.withDefaults())
                )
                .authorizeHttpRequests(authorize -> authorize
                        // --- Públicos ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Webhook é público na camada HTTP, mas o controller valida o token secreto do gateway.
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // --- Rotas de diagnóstico: nunca públicas ---
                        .requestMatchers("/api/v1/testes/**").hasRole("SUPER_ADMIN")

                        // --- Condomínio / CondoStore ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/condominios").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/condostore/pedidos").hasRole("MORADOR")

                        // --- Visitantes (POST público para Auto Check-in via QR Code na portaria) ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/visitantes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/visitantes").hasAnyRole(STAFF_OU_MORADOR)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/visitantes/*/checkin", "/api/v1/visitantes/*/checkout").hasAnyRole(STAFF)

                        // --- Encomendas ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/encomendas/minhas").hasRole("MORADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/encomendas").hasAnyRole(STAFF)
                        .requestMatchers(HttpMethod.POST, "/api/v1/encomendas").hasRole("PORTEIRO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/encomendas/*/retirar").hasAnyRole(STAFF)

                        // --- Avisos ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/avisos").hasRole("SINDICO")

                        // --- Reservas ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservas/minhas", "/api/v1/reservas/areas-comuns").hasAnyRole(STAFF_OU_MORADOR)
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservas").hasAnyRole(STAFF)
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservas").hasRole("MORADOR")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservas/*/aprovar", "/api/v1/reservas/*/rejeitar").hasAnyRole("SINDICO", "PORTEIRO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservas/*/convidados/*/checkin").hasAnyRole(STAFF)

                        // --- Chamados (Ouvidoria) ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/chamados/meus").hasRole("MORADOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/chamados").hasAnyRole(STAFF)
                        .requestMatchers(HttpMethod.POST, "/api/v1/chamados").hasAnyRole(STAFF_OU_MORADOR)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/chamados/*/escalar").hasRole("PORTEIRO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/chamados/*/status").hasAnyRole(STAFF)

                        // --- Financeiro ---
                        .requestMatchers(HttpMethod.PUT, "/api/v1/boletos/*/baixar").hasAnyRole(GESTAO)
                        .requestMatchers("/api/v1/financas/**").hasAnyRole(GESTAO)

                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "https://app.condoeconomy.com"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}


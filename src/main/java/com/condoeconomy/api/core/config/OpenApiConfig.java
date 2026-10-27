package com.condoeconomy.api.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "BearerAuth";
    public static final String TENANT_HEADER = "TenantHeader";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CondoEconomy API - Plataforma SaaS Multi-Tenant")
                        .version("1.0.0")
                        .description("API REST corporativa para gestão condominial integrada (Portaria, Finanças, Reservas, Assembleias e LGPD).")
                        .contact(new Contact()
                                .name("Suporte Técnico CondoEconomy")
                                .email("suporte@condoeconomy.com"))
                        .license(new License()
                                .name("Proprietário")
                                .url("https://app.condoeconomy.com")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(BEARER_AUTH)
                        .addList(TENANT_HEADER))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT emitido no /api/v1/auth/login. Envie no header Authorization."))
                        .addSecuritySchemes(TENANT_HEADER, new SecurityScheme()
                                .name("X-Tenant-ID")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("Identificador opcional de schema do condomínio (confinado via JWT).")));
    }
}

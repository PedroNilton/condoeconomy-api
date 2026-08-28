package com.condoeconomy.api.core.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class MultiTenantJpaConfiguration implements HibernatePropertiesCustomizer {

    private final MultiTenantConnectionProvider multiTenantConnectionProvider;
    private final CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

    public MultiTenantJpaConfiguration(
            MultiTenantConnectionProvider multiTenantConnectionProvider,
            CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
        this.multiTenantConnectionProvider = multiTenantConnectionProvider;
        this.currentTenantIdentifierResolver = currentTenantIdentifierResolver;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(org.hibernate.cfg.AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        hibernateProperties.put(org.hibernate.cfg.AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
    }
}

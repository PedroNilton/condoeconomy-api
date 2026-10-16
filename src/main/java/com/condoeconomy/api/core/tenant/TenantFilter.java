package com.condoeconomy.api.core.tenant;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TenantFilter implements Filter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final java.util.regex.Pattern TENANT_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_]{1,63}$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        String tenantId = req.getHeader(TENANT_HEADER);
        
        // Validação estrita: aceita apenas caracteres alfanuméricos e underscore (evita SQL Injection / schema tampering)
        if (tenantId != null && !tenantId.isBlank() && TENANT_PATTERN.matcher(tenantId.trim()).matches()) {
            TenantContext.setCurrentTenant(tenantId.trim().toLowerCase());
        } else {
            TenantContext.setCurrentTenant(TenantIdentifierResolver.DEFAULT_TENANT);
        }
        
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

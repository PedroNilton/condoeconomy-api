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

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        String tenantId = req.getHeader(TENANT_HEADER);
        
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setCurrentTenant(tenantId);
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

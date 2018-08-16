package com.vladmihalcea.book.hpjp.hibernate.multitenancy;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class MultiTenantConnectionProvider
        extends AbstractMultiTenantConnectionProvider {

    public static final MultiTenantConnectionProvider INSTANCE =
            new MultiTenantConnectionProvider();

    private final Map<String, ConnectionProvider> connectionProviderMap = new HashMap<>();

    Map<String, ConnectionProvider> getConnectionProviderMap() {
        return connectionProviderMap;
    }

    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        return connectionProviderMap.get(TenantContext.DEFAULT_TENANT_IDENTIFIER);
    }

    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        return connectionProviderMap.get(tenantIdentifier);
    }
}

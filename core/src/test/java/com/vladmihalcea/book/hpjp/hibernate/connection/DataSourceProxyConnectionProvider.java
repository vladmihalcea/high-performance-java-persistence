package com.vladmihalcea.book.hpjp.hibernate.connection;

import net.ttddyy.dsproxy.listener.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;

import javax.sql.DataSource;
import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class DataSourceProxyConnectionProvider extends DatasourceConnectionProviderImpl {

    @Override
    public void configure(Map configValues) {
        super.configure(configValues);
        DataSource dataSource = ProxyDataSourceBuilder
                .create(getDataSource())
                .name(getClass().getSimpleName())
                .listener(new SLF4JQueryLoggingListener())
                .build();
        super.setDataSource(dataSource);
    }
}

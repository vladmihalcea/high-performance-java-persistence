package com.vladmihalcea.util;

import com.vladmihalcea.util.logging.InlineQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.listener.logging.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import javax.sql.DataSource;

/**
 * @author Vlad Mihalcea
 */
public enum DataSourceProxyType {
    DATA_SOURCE_PROXY {
        @Override
        public DataSource dataSource(DataSource dataSource) {
            ChainListener listener = new ChainListener();
            SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
            //loggingListener.setQueryLogEntryCreator(new InlineQueryLogEntryCreator());
            listener.addListener(loggingListener);
            listener.addListener(new DataSourceQueryCountListener());
            return ProxyDataSourceBuilder
                    .create(dataSource)
                    .name(name())
                    .listener(listener)
                    .build();
        }
    };

    public abstract DataSource dataSource(DataSource dataSource);
}

package com.vladmihalcea.book.hpjp.util;

import com.p6spy.engine.spy.P6DataSource;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.listener.SLF4JQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import javax.sql.DataSource;

/**
 * <code>DataSourceProxyType</code> - DataSourceProxy Type
 *
 * @author Vlad Mihalcea
 */
public enum DataSourceProxyType {
    DATA_SOURCE_PROXY {
        @Override
        DataSource dataSource(DataSource dataSource) {
            ChainListener listener = new ChainListener();
            SLF4JQueryLoggingListener loggingListener = new SLF4JQueryLoggingListener();
            listener.addListener(new SLF4JQueryLoggingListener());
            listener.addListener(new DataSourceQueryCountListener());
            loggingListener.setQueryLogEntryCreator(new AbstractTest.InlineQueryLogEntryCreator());
            return ProxyDataSourceBuilder
                    .create(dataSource)
                    .name(name())
                    .listener(listener)
                    .build();
        }
    },
    P6SPY {
        @Override
        DataSource dataSource(DataSource dataSource) {
            return new P6DataSource(dataSource);
        }
    };

    abstract DataSource dataSource(DataSource dataSource);
}

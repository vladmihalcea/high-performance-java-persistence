package com.vladmihalcea.book.hpjp.hibernate.connection.jta;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import com.vladmihalcea.book.hpjp.util.spring.config.jta.PostgreSQLJTATransactionManagerConfiguration;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.BitronixPoolAdapter;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class JTAFlexyPoolTestConfiguration extends PostgreSQLJTATransactionManagerConfiguration {

    @Override
    protected Class configurationClass() {
        return FlexyPoolEntities.class;
    }

    @Override
    public DataSource actualDataSource() {
        final PoolingDataSource poolingDataSource = (PoolingDataSource) super.actualDataSource();
        com.vladmihalcea.flexypool.config.Configuration<PoolingDataSource> configuration = new com.vladmihalcea.flexypool.config.Configuration.Builder<>(
            getClass().getSimpleName(), poolingDataSource, BitronixPoolAdapter.FACTORY).build();

        FlexyPoolDataSource<PoolingDataSource> flexyPoolDataSource = new FlexyPoolDataSource<PoolingDataSource>(configuration) {
            @Override
            public void start() {
                poolingDataSource.init();
                super.start();
            }

            @Override
            public void stop() {
                super.stop();
                poolingDataSource.close();
            }
        };
        return flexyPoolDataSource;
    }
}

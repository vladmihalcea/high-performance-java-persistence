package com.vladmihalcea.book.hpjp.hibernate.connection;

import com.vladmihalcea.book.hpjp.hibernate.connection.jta.FlexyPoolEntities;
import com.vladmihalcea.book.hpjp.util.spring.config.jpa.PostgreSQLJpaConfiguration;
import com.vladmihalcea.flexypool.FlexyPoolDataSource;
import com.vladmihalcea.flexypool.adaptor.DataSourcePoolAdapter;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlexyPoolTestConfiguration extends PostgreSQLJpaConfiguration {

    @Override
    protected Class configurationClass() {
        return FlexyPoolEntities.class;
    }

    @Override
    public DataSource actualDataSource() {
        final DataSource dataSource = super.actualDataSource();
        com.vladmihalcea.flexypool.config.Configuration<DataSource> configuration = new com.vladmihalcea.flexypool.config.Configuration.Builder<>(
            getClass().getSimpleName(), dataSource, DataSourcePoolAdapter.FACTORY).build();
        FlexyPoolDataSource flexyPoolDataSource = new FlexyPoolDataSource<>(configuration);
        return flexyPoolDataSource;
    }
}

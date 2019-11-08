/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vladmihalcea.book.hpjp.util.spring.routing;

import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.spring.config.jpa.AbstractJPAConfiguration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "com.vladmihalcea.book.hpjp.util.spring.routing")
@PropertySource({"/META-INF/jdbc-postgresql-replication.properties"})
public class TransactionRoutingConfiguration extends AbstractJPAConfiguration {

    @Value("${jdbc.url.primary}")
    private String primaryUrl;

    @Value("${jdbc.url.replica}")
    private String replicaUrl;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    @Bean
    public DataSource readWriteDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(primaryUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return connectionPoolDataSource(dataSource);
    }

    @Bean
    public DataSource readOnlyDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(replicaUrl);
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return connectionPoolDataSource(dataSource);
    }

    @Bean
    public TransactionRoutingDataSource actualDataSource() {
        TransactionRoutingDataSource routingDataSource = new TransactionRoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(DataSourceType.READ_WRITE, readWriteDataSource());
        dataSourceMap.put(DataSourceType.READ_ONLY, readOnlyDataSource());

        routingDataSource.setTargetDataSources(dataSourceMap);
        return routingDataSource;
    }

    @Override
    protected Properties additionalProperties() {
        Properties properties = super.additionalProperties();
        properties.setProperty("hibernate.connection.provider_disables_autocommit", Boolean.TRUE.toString());
        return properties;
    }

    @Override
    protected String[] packagesToScan() {
        return new String[]{
            "com.vladmihalcea.book.hpjp.hibernate.transaction.forum"
        };
    }

    @Override
    protected String databaseType() {
        return Database.POSTGRESQL.name().toLowerCase();
    }

    protected HikariConfig hikariConfig(DataSource dataSource) {
        HikariConfig hikariConfig = new HikariConfig();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        hikariConfig.setMaximumPoolSize(cpuCores * 4);
        hikariConfig.setDataSource(dataSource);

        hikariConfig.setAutoCommit(false);
        return hikariConfig;
    }

    protected HikariDataSource connectionPoolDataSource(DataSource dataSource) {
        return new HikariDataSource(hikariConfig(dataSource));
    }
}

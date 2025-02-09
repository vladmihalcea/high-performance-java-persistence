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

package com.vladmihalcea.hpjp.spring.transaction.routing;

import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.spring.config.jpa.AbstractJPAConfiguration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "com.vladmihalcea.hpjp.spring.transaction.routing")
@PropertySource("/META-INF/jdbc-postgresql-replication.properties")
public class TransactionRoutingConfiguration extends AbstractJPAConfiguration {

    @Value("${jdbc.url.primary}")
    private String primaryUrl;

    @Value("${jdbc.url.replica}")
    private String replicaUrl;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    public TransactionRoutingConfiguration() {
        super(Database.POSTGRESQL);
    }

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
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                create sequence if not exists hibernate_sequence start 1 increment 1;
                create table if not exists post (id int8 not null, title varchar(255), primary key (id));
                create table if not exists post_comment (id int8 not null, review varchar(255), post_id int8, primary key (id));
                create table if not exists post_details (created_by varchar(255), created_on timestamp, post_id int8 not null, primary key (post_id));
                create table if not exists post_tag (post_id int8 not null, tag_id int8 not null);
                create table if not exists tag (id int8 not null, name varchar(255), primary key (id));
                alter table if exists post_comment drop constraint if exists FKna4y825fdc5hw8aow65ijexm0;
                alter table if exists post_details drop constraint if exists FKmcgdm1k7iriyxsq4kukebj4ei;
                alter table if exists post_tag drop constraint if exists FKac1wdchd2pnur3fl225obmlg0;
                alter table if exists post_tag drop constraint if exists FKc2auetuvsec0k566l0eyvr9cs;
                alter table if exists post_comment add constraint FKna4y825fdc5hw8aow65ijexm0 foreign key (post_id) references post;
                alter table if exists post_details add constraint FKmcgdm1k7iriyxsq4kukebj4ei foreign key (post_id) references post;
                alter table if exists post_tag add constraint FKac1wdchd2pnur3fl225obmlg0 foreign key (tag_id) references tag;
                alter table if exists post_tag add constraint FKc2auetuvsec0k566l0eyvr9cs foreign key (post_id) references post;
                """);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
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
        properties.setProperty(
            "hibernate.connection.provider_disables_autocommit",
            Boolean.TRUE.toString()
        );
        return properties;
    }

    @Override
    protected String[] packagesToScan() {
        return new String[]{
            "com.vladmihalcea.hpjp.hibernate.transaction.forum"
        };
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

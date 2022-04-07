package com.vladmihalcea.book.hpjp.hibernate.multitenancy;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.junit.Test;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class SchemaMultitenancyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                User.class,
                Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
        properties.setProperty(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, TenantContext.TenantIdentifierResolver.class.getName());
        properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, MultiTenantConnectionProvider.INSTANCE);
    }

    @Override
    public void afterInit() {
        PGSimpleDataSource defaultDataSource = (PGSimpleDataSource) database().dataSourceProvider().dataSource();
        addTenantConnectionProvider(TenantContext.DEFAULT_TENANT_IDENTIFIER, defaultDataSource, propertiesMap());

        createSchema("europe");
        createSchema("asia");
    }

    private void createSchema(String schemaName) {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate(String.format("drop schema if exists %s cascade", schemaName));

                    statement.executeUpdate(String.format("create schema %s", schemaName));

                    statement.executeUpdate(String.format("SET search_path TO %s,public", schemaName));

                    statement.executeUpdate("create sequence hibernate_sequence start 1 increment 1");
                    statement.executeUpdate("create table posts (id int8 not null, created_on timestamp, title varchar(255), user_id int8, primary key (id))");
                    statement.executeUpdate("create table users (id int8 not null, registered_on timestamp, firstName varchar(255), lastName varchar(255), primary key (id))");
                    statement.executeUpdate("alter table if exists posts add constraint fk_user_id foreign key (user_id) references users");

                    statement.executeUpdate("SET search_path TO public");
                }
            });
        });

        addTenantConnectionProvider(schemaName);
    }

    private void addTenantConnectionProvider(String tenantId) {
        PGSimpleDataSource defaultDataSource = (PGSimpleDataSource) database().dataSourceProvider().dataSource();

        Map<String, Object> properties = propertiesMap();

        PGSimpleDataSource tenantDataSource = new PGSimpleDataSource();
        tenantDataSource.setDatabaseName(defaultDataSource.getDatabaseName());
        tenantDataSource.setCurrentSchema(tenantId);
        tenantDataSource.setServerName(defaultDataSource.getServerName());
        tenantDataSource.setUser(defaultDataSource.getUser());
        tenantDataSource.setPassword(defaultDataSource.getPassword());

        properties.put(
                Environment.DATASOURCE,
                dataSourceProxyType().dataSource(tenantDataSource)
        );

        addTenantConnectionProvider(tenantId, tenantDataSource, properties);
    }
    
    private void addTenantConnectionProvider(String tenantId, DataSource tenantDataSource, Map<String, Object> properties) {
        DatasourceConnectionProviderImpl connectionProvider = new DatasourceConnectionProviderImpl();
        connectionProvider.setDataSource(tenantDataSource);
        connectionProvider.configure(properties);
        MultiTenantConnectionProvider.INSTANCE.getConnectionProviderMap().put(
                tenantId, connectionProvider
        );
    }

    @Test
    public void test() {
        TenantContext.setTenant("europe");

        User vlad = doInJPA(entityManager -> {

            LOGGER.info(
                    "Current schema: {}",
                    entityManager.createNativeQuery("select current_schema()").getSingleResult()
            );

            User user = new User();
            user.setFirstName("Vlad");
            user.setLastName("Mihalcea");

            entityManager.persist(user);

            return user;
        });

        TenantContext.setTenant("asia");

        doInJPA(entityManager -> {

            LOGGER.info(
                    "Current schema: {}",
                    entityManager.createNativeQuery("select current_schema()").getSingleResult()
            );

            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            entityManager.persist(user);
        });

        TenantContext.setTenant("europe");

        doInJPA(entityManager -> {

            LOGGER.info(
                    "Current schema: {}",
                    entityManager.createNativeQuery("select current_schema()").getSingleResult()
            );

            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");
            post.setUser(vlad);
            entityManager.persist(post);
        });
    }

    @Entity(name = "User")
    @Table(name = "users")
    public static class User {

        @Id
        @GeneratedValue
        private Long id;

        private String firstName;

        private String lastName;

        @Column(name = "registered_on")
        @CreationTimestamp
        private LocalDateTime createdOn;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
        }
    }

    @Entity(name = "Post")
    @Table(name = "posts")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @Column(name = "created_on")
        @CreationTimestamp
        private LocalDateTime createdOn;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id")
        private User user;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }
    }

}

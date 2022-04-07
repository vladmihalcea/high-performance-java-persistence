package com.vladmihalcea.book.hpjp.hibernate.multitenancy;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class CatalogMultitenancyTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                User.class,
                Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
        properties.setProperty(AvailableSettings.SHOW_SQL, "true");
        properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, MultiTenantConnectionProvider.INSTANCE);
        properties.setProperty(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, TenantContext.TenantIdentifierResolver.class.getName());
    }

    @Override
    public void afterInit() {
        MysqlDataSource defaultDataSource = (MysqlDataSource) database().dataSourceProvider().dataSource();
        addTenantConnectionProvider(TenantContext.DEFAULT_TENANT_IDENTIFIER, defaultDataSource, propertiesMap());

        createCatalog("europe");
        createCatalog("asia");
    }

    private void createCatalog(String catalogName) {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try(Statement statement = connection.createStatement()) {
                    statement.executeUpdate(String.format("drop database if exists %s", catalogName));

                    statement.executeUpdate(String.format("create database %s", catalogName));

                    statement.executeUpdate(String.format("USE %s", catalogName));

                    statement.executeUpdate("create table posts (id bigint not null auto_increment, created_on datetime(6), title varchar(255), user_id bigint, primary key (id)) engine=InnoDB");
                    statement.executeUpdate("create table users (id bigint not null auto_increment, registered_on datetime(6), firstName varchar(255), lastName varchar(255), primary key (id)) engine=InnoDB");
                    statement.executeUpdate("alter table posts add constraint fk_user_id foreign key (user_id) references users (id)");
                }
            });
        });

        addTenantConnectionProvider(catalogName);
    }

    private void addTenantConnectionProvider(String tenantId) {
        DataSourceProvider dataSourceProvider = database().dataSourceProvider();

        Map<String, Object> properties = propertiesMap();

        MysqlDataSource tenantDataSource = new MysqlDataSource();
        tenantDataSource.setDatabaseName(tenantId);
        tenantDataSource.setUser(dataSourceProvider.username());
        tenantDataSource.setPassword(dataSourceProvider.password());

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

            User user = new User();
            user.setFirstName("Vlad");
            user.setLastName("Mihalcea");

            entityManager.persist(user);

            return user;
        });

        TenantContext.setTenant("asia");

        doInJPA(entityManager -> {

            User user = new User();
            user.setFirstName("John");
            user.setLastName("Doe");

            entityManager.persist(user);
        });

        TenantContext.setTenant("europe");

        doInJPA(entityManager -> {

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
        @GeneratedValue(strategy = GenerationType.IDENTITY)
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
        @GeneratedValue(strategy = GenerationType.IDENTITY)
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

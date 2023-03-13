package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.providers.CockroachDBDataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class HikariCPCockroachDBConnectionProviderTest extends DriverConnectionProviderTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class
        };
    }

    protected DataSourceProvider dataSourceProvider() {
        return new CockroachDBDataSourceProvider();
    }

    @Override
    public void init() {
        if(!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        super.init();
    }

    @Override
    protected void appendDriverProperties(Properties properties) {
        DataSourceProvider dataSourceProvider = dataSourceProvider();
        properties.put("hibernate.connection.provider_class", HikariCPConnectionProvider.class.getName());
        properties.put("hibernate.hikari.minimumPoolSize", "1");
        properties.put("hibernate.hikari.maximumPoolSize", "2");
        properties.put("hibernate.hikari.transactionIsolation", "TRANSACTION_SERIALIZABLE");
        properties.put("hibernate.hikari.dataSourceClassName", dataSourceProvider.dataSourceClassName().getName());
        properties.put("hibernate.hikari.dataSource.url", dataSourceProvider.url());
        properties.put("hibernate.hikari.dataSource.user", dataSourceProvider.username());
        properties.put("hibernate.hikari.dataSource.password", dataSourceProvider.password());
    }

    @Test
    public void testConnection() {
        if (!ENABLE_LONG_RUNNING_TESTS) {
            return;
        }
        for (final AtomicLong i = new AtomicLong(); i.get() < 5; i.incrementAndGet()) {
            doInJPA(em -> {
                em.persist(new Post(i.get()));
            });
        }

        doInJPA(em -> {
            Post post = em.find(Post.class, 1L);
        });
        doInJPA(em -> {
            em.createQuery("select p from Post p", Post.class).getResultList();
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

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
    }
}

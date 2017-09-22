package com.vladmihalcea.book.hpjp.hibernate.connection;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.providers.CockroachDBDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

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

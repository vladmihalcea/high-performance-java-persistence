package com.vladmihalcea.hpjp.hibernate.connection;

import com.vladmihalcea.hpjp.util.AbstractTest;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UserSuppliedConnectionProviderTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{Post.class};
    }

    protected Properties properties() {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", dataSourceProvider().hibernateDialect());
        properties.put("hibernate.boot.allow_jdbc_metadata_access", "false");
        return properties;
    }

    @Test
    public void testConnection() {
        try(Connection connection = dataSource().getConnection()) {
            executeStatement("create table post (id bigint not null, title varchar(255), primary key (id))");
            Session session = sessionFactory().withOptions().connection(connection).openSession();

            session.getTransaction().begin();

            int postCount = 5;
            for (final AtomicLong i = new AtomicLong(); i.get() < postCount; i.incrementAndGet()) {
                session.persist(new Post().setId(i.get()));
            }
            session.getTransaction().commit();

            session.getTransaction().begin();
            Post post = session.find(Post.class, 1L);
            post.setTitle("High-Performance Java Persistence");
            session.getTransaction().commit();

            session.getTransaction().begin();
            Post _post = session.createQuery("""
                select p 
                from Post p
                where p.id = :id
                """, Post.class)
            .setParameter("id", 1L)
            .getSingleResult();
            assertEquals("High-Performance Java Persistence", _post.getTitle());
            session.getTransaction().commit();
        } catch (SQLException e) {
            fail(e.getMessage());
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }
}

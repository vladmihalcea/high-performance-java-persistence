package com.vladmihalcea.hpjp.hibernate.logging;

import java.util.Properties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.junit.Test;

import com.vladmihalcea.hpjp.util.AbstractTest;

/**
 * @author Vlad Mihalcea
 */
public class HibernateLoggingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "5");
        //properties.put("hibernate.format_sql", "true");
    }

    @Override
    protected boolean proxyDataSource() {
        return false;
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
            );
        });
    }

    @Test
    public void testBatch() {
        doInJPA(entityManager -> {
            for (long id = 1; id <= 5; id++) {
                entityManager.persist(
                    new Post()
                        .setId(id)
                        .setTitle(
                            String.format(
                                "High-Performance Java Persistence, part %d",
                                id
                            )
                        )
                );
            }
        });
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

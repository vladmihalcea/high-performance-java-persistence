package com.vladmihalcea.hpjp.hibernate.logging;

import com.vladmihalcea.hpjp.util.AbstractTest;
import io.hypersistence.utils.hibernate.query.QueryStackTraceLogger;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class QueryStackTraceLoggerTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            AvailableSettings.STATEMENT_INSPECTOR,
            new QueryStackTraceLogger("com.vladmihalcea.hpjp")
        );
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("Post it!");

            entityManager.persist(post);
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

        @Version
        private short version;

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

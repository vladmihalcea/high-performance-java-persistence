package com.vladmihalcea.book.hpjp.hibernate.query.timeout;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import org.hibernate.jpa.QueryHints;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerServerSideTimeoutTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(
            QueryHints.SPEC_HINT_TIMEOUT, String.valueOf(1000)
        );
    }

    @Test
    public void testQueryTimeout() {
        try {
            executeStatement("EXEC sp_configure 'remote query timeout', 1");
            executeStatement("RECONFIGURE");

           doInJPA(entityManager -> {
                Post post = new Post();
                post.setTitle("High-Performance Java Persistence");

                entityManager.persist(post);
                return post.getId();
            });

            doInJPA(entityManager -> {
                List<Post> posts = entityManager
                    .createQuery("SELECT p from Post p")
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();

                //executeStatement(entityManager, "WAITFOR DELAY '00:00:02'");
                doInJPA(_entityManager -> {
                    LOGGER.info("Start waiting");

                    List<Post> posts_ = _entityManager
                        .createQuery("SELECT p from Post p")
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .getResultList();

                    LOGGER.info("Done waiting");
                });
            });
        } catch (Exception expected) {
            LOGGER.info("Query timed out", expected);
        } finally {
            executeStatement("EXEC sp_configure 'remote query timeout', 0");
            executeStatement("RECONFIGURE");
        }
    }

    @Test
    public void testTimeout() {
        try {
            executeStatement("EXEC sp_configure 'remote query timeout', 1");
            executeStatement("RECONFIGURE");

            doInJPA(entityManager -> {
                LOGGER.info("Start waiting");
                executeStatement(entityManager, "WAITFOR DELAY '00:00:02'");

                LOGGER.info("Done waiting");
            });
        } finally {
            executeStatement("EXEC sp_configure 'remote query timeout', 0");
            executeStatement("RECONFIGURE");
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Integer id;

        private String title;

        public Integer getId() {
            return id;
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

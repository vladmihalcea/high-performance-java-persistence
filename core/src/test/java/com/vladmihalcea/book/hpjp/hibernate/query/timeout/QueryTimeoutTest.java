package com.vladmihalcea.book.hpjp.hibernate.query.timeout;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.NativeQuery;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class QueryTimeoutTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testJPQLTimeoutHint() {

        doInJPA(entityManager -> {
            for (int i = 0; i < 5; i++) {
                entityManager.persist(
                    new Post().setTitle(String.format("Hibernate User Guide, Chapter %d", i + 1))
                );
            }

            for (int i = 0; i < 5; i++) {
                entityManager.persist(
                    new Post().setTitle(String.format("%d Hibernate Tips", (i + 1) * 5))
                );
            }

            for (int i = 0; i < 5; i++) {
                entityManager.persist(
                        new Post().setTitle(String.format("%d Tips to master Hibernate", (i + 1) * 10))
                );
            }
        });

        doInJPA(entityManager -> {
            List<Post> posts = entityManager
            .createQuery(
                "select p " +
                "from Post p " +
                "where lower(p.title) like lower(:titlePattern)", Post.class)
            .setParameter("titlePattern", "%Hibernate%")
            .setHint("jakarta.persistence.query.timeout", 50)
            .getResultList();

            assertEquals(15, posts.size());
        });

        doInJPA(entityManager -> {
            List<Post> posts = entityManager
            .createQuery(
                "select p " +
                "from Post p " +
                "where lower(p.title) like lower(:titlePattern)", Post.class)
            .setParameter("titlePattern", "%Hibernate%")
            .setHint("org.hibernate.timeout", 1)
            .getResultList();

            assertEquals(15, posts.size());
        });

        doInJPA(entityManager -> {
            List<Post> posts = entityManager
            .createQuery(
                "select p " +
                "from Post p " +
                "where lower(p.title) like lower(:titlePattern)", Post.class)
            .setParameter("titlePattern", "%Hibernate%")
            .unwrap(org.hibernate.query.Query.class)
            .setTimeout(1)
            .getResultList();

            assertEquals(15, posts.size());
        });
    }

    @Test
    public void testJPATimeout() {
        doInJPA(entityManager -> {
            try {
                List<Tuple> result = entityManager
                .createNativeQuery(
                    "SELECT 1 " +
                    "FROM pg_sleep(2) ", Tuple.class)
                .setHint("jakarta.persistence.query.timeout", (int) TimeUnit.SECONDS.toMillis(1))
                .getResultList();

                fail("Timeout failure expected");
            } catch (Exception e) {
                PSQLException rootCause = ExceptionUtil.rootCause(e);
                assertTrue(rootCause.getMessage().contains("canceling statement due to user request"));            }
        });
    }

    @Test
    public void testHibernateTimeout() {
        doInJPA(entityManager -> {
            try {
                List<Tuple> result = entityManager
                .createNativeQuery(
                    "SELECT 1 " +
                    "FROM pg_sleep(2) ", Tuple.class)
                .unwrap(NativeQuery.class)
                .setTimeout(1)
                .getResultList();

                fail("Timeout failure expected");
            } catch (Exception e) {
                PSQLException rootCause = ExceptionUtil.rootCause(e);
                assertTrue(rootCause.getMessage().contains("canceling statement due to user request"));
            }
        });
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

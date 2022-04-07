package com.vladmihalcea.book.hpjp.hibernate.query.timeout;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.jpa.QueryHints;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SessionQueryTimeoutTest extends AbstractTest {

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

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(
            QueryHints.SPEC_HINT_TIMEOUT, String.valueOf(1000)
        );
    }

    @Test
    public void testJPATimeout() {
        doInJPA(entityManager -> {
            try {
                List<Post> posts = entityManager
                .createQuery(
                    "select p " +
                    "from Post p " +
                    "where function('1 >= ALL ( SELECT 1 FROM pg_locks, pg_sleep(2) ) --',) is ''", Post.class)
                .getResultList();

                fail("Timeout failure expected");
            } catch (Exception e) {
                PSQLException rootCause = ExceptionUtil.rootCause(e);
                assertTrue(rootCause.getMessage().contains("canceling statement due to user request"));            }
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

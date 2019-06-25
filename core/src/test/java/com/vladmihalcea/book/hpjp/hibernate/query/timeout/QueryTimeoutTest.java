package com.vladmihalcea.book.hpjp.hibernate.query.timeout;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.NativeQuery;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import javax.persistence.*;
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
    public void testJPATimeout() {
        doInJPA(entityManager -> {
            try {
                List<Tuple> result = entityManager
                .createNativeQuery(
                    "SELECT 1 " +
                    "FROM pg_sleep(2) ", Tuple.class)
                .setHint("javax.persistence.query.timeout", (int) TimeUnit.SECONDS.toMillis(1))
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
        private Integer id;

        private String title;

        public Post() {}

        public Post(String title) {
            this.title = title;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
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

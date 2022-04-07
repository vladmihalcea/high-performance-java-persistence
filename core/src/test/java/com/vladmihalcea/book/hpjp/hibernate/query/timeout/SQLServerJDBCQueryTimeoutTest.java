package com.vladmihalcea.book.hpjp.hibernate.query.timeout;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerJDBCQueryTimeoutTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Test
    public void testQueryTimeout() {
        try {
            doInJPA(entityManager -> {
                LOGGER.info("Start waiting");
                //Works for any query
                entityManager.unwrap(Session.class).doWork(connection -> {
                    connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), 1000);
                    executeStatement(connection, "WAITFOR DELAY '00:00:02'");
                    fail("Should have thrown a query timeout!");
                });
                LOGGER.info("Done waiting");
            });
        } catch (Exception e) {
            LOGGER.info("Timeout triggered", e);
            assertTrue(ExceptionUtil.rootCause(e).getMessage().contains("Read timed out"));
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

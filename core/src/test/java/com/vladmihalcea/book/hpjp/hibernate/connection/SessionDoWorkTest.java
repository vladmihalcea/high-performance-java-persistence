package com.vladmihalcea.book.hpjp.hibernate.connection;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.SocketTimeoutException;
import java.sql.Connection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class SessionDoWorkTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    private int connectionPoolSize = 4;

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testDoWork() {
        Executor executor = Executors.newFixedThreadPool(connectionPoolSize);

        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                connection.setNetworkTimeout(
                    executor,
                    (int) TimeUnit.SECONDS.toMillis(1)
                );
            });

            try {
                entityManager.createNativeQuery("select pg_sleep(2)").getResultList();
            } catch (Exception e) {
                assertTrue(SocketTimeoutException.class.isInstance(ExceptionUtil.rootCause(e)));
            }
        });
    }

    @Test
    public void testDoReturningWork() {
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            int isolationLevel = session.doReturningWork(
                connection -> connection.getTransactionIsolation()
            );

            assertEquals(Connection.TRANSACTION_READ_COMMITTED, isolationLevel);
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

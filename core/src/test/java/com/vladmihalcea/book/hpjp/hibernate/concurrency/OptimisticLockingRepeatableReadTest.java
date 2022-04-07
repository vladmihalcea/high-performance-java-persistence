package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class OptimisticLockingRepeatableReadTest extends AbstractPostgreSQLIntegrationTest {

    private final CountDownLatch loadPostLatch = new CountDownLatch(2);
    private final CountDownLatch aliceLatch = new CountDownLatch(1);

    public class AliceTransaction implements Runnable {

        @Override
        public void run() {
            try {
                doInJPA(entityManager -> {
                    try {
                        entityManager.unwrap(Session.class).doWork(connection -> {
                            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, connection.getTransactionIsolation());
                        });

                        Post post = entityManager.find(Post.class, 1L);
                        loadPostLatch.countDown();
                        loadPostLatch.await();
                        post.setTitle("JPA");
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (StaleObjectStateException expected) {
                LOGGER.info("Alice: Optimistic locking failure", expected);
            } catch (Exception unexpected) {
                LOGGER.info("Alice: Optimistic locking failure due to MVCC", unexpected);
            }
            aliceLatch.countDown();
        }
    }

    public class BobTransaction implements Runnable {

        @Override
        public void run() {
            try {
                doInJPA(entityManager -> {
                    try {
                        entityManager.unwrap(Session.class).doWork(connection -> {
                            assertEquals(Connection.TRANSACTION_REPEATABLE_READ, connection.getTransactionIsolation());
                        });
                        Post post = entityManager.find(Post.class, 1L);
                        loadPostLatch.countDown();
                        loadPostLatch.await();
                        aliceLatch.await();
                        post.incrementLikes();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                });
            } catch (StaleObjectStateException expected) {
                LOGGER.info("Bob: Optimistic locking failure", expected);
            } catch (Exception unexpected) {
                LOGGER.info("Bob: Optimistic locking failure due to MVCC", unexpected);
            }
        }
    }

    @Override
    protected boolean connectionPooling() {
        return true;
    }

    protected HikariDataSource connectionPoolDataSource(DataSource dataSource) {
        HikariConfig hikariConfig = new HikariConfig();
        int cpuCores = Runtime.getRuntime().availableProcessors();
        hikariConfig.setMaximumPoolSize(cpuCores * 4);
        hikariConfig.setDataSource(dataSource);
        hikariConfig.setTransactionIsolation("TRANSACTION_REPEATABLE_READ");
        return new HikariDataSource(hikariConfig);
    }

    @Test
    public void testOptimisticLocking() throws InterruptedException {

        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("JDBC");
            entityManager.persist(post);
        });

        Thread alice = new Thread(new AliceTransaction());
        alice.setName("Alice");
        Thread bob = new Thread(new BobTransaction());
        bob.setName("Bob");

        alice.start();
        bob.start();

        alice.join();
        bob.join();
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Entity(name = "Post") @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        private long views;

        private int likes;

        @Version
        private int version;

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

        public long getViews() {
            return views;
        }

        public int getLikes() {
            return likes;
        }

        public int incrementLikes() {
            return ++likes;
        }

        public void setViews(long views) {
            this.views = views;
        }

        public int getVersion() {
            return version;
        }
    }
}

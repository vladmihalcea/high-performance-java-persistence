package com.vladmihalcea.hpjp.hibernate.logging;

import com.vladmihalcea.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import jakarta.persistence.*;
import org.hibernate.LockOptions;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.Properties;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class MySQLMDCLoggingTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "5");
    }

    @Override
    protected boolean proxyDataSource() {
        return true;
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("Post it!");

            entityManager.persist(post);
        });
    }

    @Test
    public void testWithMDC() {
        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p
                from Post p
                where p.id = :id
                """, Post.class)
                .setParameter("id", 1L)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();

            try (MDC.MDCCloseable closable = MDC.putCloseable(
                "txId",
                String.format(" TxId: [%s]", transactionId(entityManager))
            )) {
                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        LOGGER.info("Acquire lock so that the TxId is assigned");
                        _entityManager.persist(
                            new Post()
                                .setId(2L)
                                .setTitle("New Post!")
                        );
                        _entityManager.flush();
                        sleep(100);

                        try (MDC.MDCCloseable _closable = MDC.putCloseable(
                            "txId",
                            String.format(" TxId: [%s]", transactionId(_entityManager))
                        )) {

                            try {
                                Post _post = (Post) _entityManager.createQuery("""
                                    select p
                                    from Post p
                                    where p.id = :id
                                    """, Post.class)
                                    .setParameter("id", 1L)
                                    .unwrap(org.hibernate.query.Query.class)
                                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                                    .setHint(
                                        "jakarta.persistence.lock.timeout",
                                        LockOptions.NO_WAIT
                                    )
                                    .getSingleResult();
                            } catch (Exception expected) {
                                assertTrue(ExceptionUtil.isLockTimeout(expected));
                            }
                        }
                    });
                });
            }
        });
    }

    protected String threadId(EntityManager entityManager) {
        return String.valueOf(
            entityManager
                .createNativeQuery("SELECT CONNECTION_ID()")
                .getSingleResult()
        );
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

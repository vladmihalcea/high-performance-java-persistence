package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Vlad Mihalcea
 */
public class MySQLBatchInsertLoadTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "5");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Test
    public void test() {
        LOGGER.info("testInsertPosts");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                try (PreparedStatement st = connection.prepareStatement("""
                            INSERT INTO post (title)
                            VALUES (?)
                            """)) {
                    for (long i = 1; i <= 3; i++) {
                        st.setString(1, String.format("High-Performance Java Persistence, Part %d", i));
                        st.addBatch();
                    }
                    st.executeBatch();
                }
            });
            session.createQuery("select p from Post p").getResultList();
        });
    }

    @Test
    public void testTransactionless() {
        EntityManager entityManager = entityManagerFactory().createEntityManager();

        for (long i = 1; i <= 3; i++) {
            entityManager.persist(
                new Post()
                    .setTitle(
                        String.format(
                            "High-Performance Java Persistence, part %d",
                            i
                        )
                    )
            );
        }

        try {
            entityManager.getTransaction().begin();
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            entityManager.getTransaction().rollback();
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
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

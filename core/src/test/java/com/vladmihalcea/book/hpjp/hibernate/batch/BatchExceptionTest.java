package com.vladmihalcea.book.hpjp.hibernate.batch;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.Test;

import javax.persistence.*;
import java.sql.*;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BatchExceptionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "5");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }

    @Test
    public void testInsertConstraintViolation() {
        LOGGER.info("testInsertPosts");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {

                try (PreparedStatement st = connection.prepareStatement(
                        "INSERT INTO post (id, title) " +
                                "VALUES (?, ?)")) {
                    for (long i = 0; i < 5; i++) {
                        st.setLong(1, i % 2);
                        st.setString(2, String.format("High-Performance Java Persistence, Part %d", i));
                        st.addBatch();
                    }
                    st.executeBatch();
                } catch (BatchUpdateException e) {
                    LOGGER.info("Batch has managed to process {} entries", e.getUpdateCounts().length);
                }
            });

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

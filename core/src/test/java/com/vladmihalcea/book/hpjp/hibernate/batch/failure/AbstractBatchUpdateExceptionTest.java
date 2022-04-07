package com.vladmihalcea.book.hpjp.hibernate.batch.failure;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractBatchUpdateExceptionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Test
    public void testInsertConstraintViolation() {
        LOGGER.info("testInsertPosts");
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            session.doWork(connection -> {
                try (PreparedStatement st = connection.prepareStatement("""
                        INSERT INTO post (id, title)
                        VALUES (?, ?)
                        """)) {
                    for (long i = 1; i <= 3; i++) {
                        st.setLong(1, i % 2);
                        st.setString(2, String.format("High-Performance Java Persistence, Part %d", i));
                        st.addBatch();
                    }
                    st.executeBatch();
                } catch (BatchUpdateException e) {
                    onBatchUpdateException(e);
                }
            });
        });
    }

    protected abstract void onBatchUpdateException(BatchUpdateException e);

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

package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.*;
import org.junit.Test;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Parameter;
import javax.persistence.Table;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class BatchProcessingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "25");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }

    @Test
    public void testFlushClearCommit() {
        int entityCount = 50;
        int batchSize = 25;

        EntityManager entityManager = entityManagerFactory().createEntityManager();

        try {
            entityManager.getTransaction().begin();

            for (int i = 0; i < entityCount; ++i) {
                if (i > 0 && i % batchSize == 0) {
                    flush(entityManager);
                }

                Post post = new Post(String.format("Post %d", i + 1));
                entityManager.persist(post);
            }

            entityManager.getTransaction().commit();
        } catch (RuntimeException e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        } finally {
            entityManager.close();
        }
    }

    private void flush(EntityManager entityManager) {
        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();

        entityManager.clear();
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(generator = "seq_post")
        @SequenceGenerator(
            name = "seq_post",
            sequenceName = "seq_post",
            allocationSize = 25
        )
        private Long id;

        private String title;


        public Post() {
        }

        public Post(String title) {
            this.title = title;
        }

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

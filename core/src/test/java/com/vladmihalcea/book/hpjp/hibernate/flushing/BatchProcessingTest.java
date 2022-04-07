package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
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

                Post post = new Post().setTitle(String.format("Post %d", i + 1));
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
        //Commit triggers a flush when using FlushType.AUTO
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

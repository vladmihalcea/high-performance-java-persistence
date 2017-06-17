package com.vladmihalcea.book.hpjp.hibernate.flushing;

import java.util.Properties;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class BatchProcessingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
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
    public void testBatchProcessing() {
        int entityCount = 50;
        int batchSize = 25;

        EntityManager entityManager = entityManagerFactory().createEntityManager();;

        try {
            entityManager.getTransaction().begin();

            for ( int i = 0; i < entityCount; ++i ) {
                if ( i > 0 && i % batchSize == 0 ) {
                    flush( entityManager );
                }

                Post post = new Post( String.format( "Post %d", i + 1 ) );
                entityManager.persist( post );
            }

            entityManager.getTransaction().commit();
        } catch (RuntimeException e) {
            if ( entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw e;
        } finally {
            entityManager.close();
        }
    }

    private void flush(EntityManager entityManager) {
        entityManager.flush();
        entityManager.clear();

        entityManager.getTransaction().commit();
        entityManager.getTransaction().begin();
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
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

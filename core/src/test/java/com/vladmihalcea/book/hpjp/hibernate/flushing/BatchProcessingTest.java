package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

/**
 * <code>BatchProcessingTest</code> - Batch processing test
 *
 * @author Vlad Mihalcea
 */
public class BatchProcessingTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class
        };
    }

    @Test
    public void testBatchProcessing() {
        int entityCount = 100;
        int batchSize = 25;

        EntityManager entityManager = null;
        EntityTransaction transaction = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();

            transaction = entityManager.getTransaction();
            transaction.begin();

            for ( int i = 0; i < entityCount; ++i ) {
                Post post = new Post( String.format( "Post %d", i ) );
                entityManager.persist( post );
                if ( i % batchSize == 0 ) {
                    entityManager.flush();
                    entityManager.clear();

                    transaction.commit();
                    transaction.begin();
                }
            }

            transaction.commit();
        } catch (RuntimeException e) {
            if ( transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
        //end::batch-session-batch-insert-example[]
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

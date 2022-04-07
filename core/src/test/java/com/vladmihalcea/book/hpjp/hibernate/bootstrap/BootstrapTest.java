package com.vladmihalcea.book.hpjp.hibernate.bootstrap;

import com.vladmihalcea.book.hpjp.util.transaction.JPATransactionVoidFunction;
import org.junit.Test;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
public class BootstrapTest extends AbstractJPAProgrammaticBootstrapTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class
        };
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

    @Test
    public void test() {
        doInJPA(entityManager -> {
            for (long id = 1; id <= 3; id++) {
                Post post = new Post();
                post.setId(id);
                post.setTitle(
                    String.format(
                        "High-Performance Java Persistence, Part %d", id
                    )
                );
                entityManager.persist(post);
            }
        });
    }

    protected void doInJPA(JPATransactionVoidFunction function) {
        EntityManager entityManager = null;
        EntityTransaction txn = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            function.beforeTransactionCompletion();
            txn = entityManager.getTransaction();
            txn.begin();
            function.accept(entityManager);
            if ( !txn.getRollbackOnly() ) {
                txn.commit();
            }
            else {
                try {
                    txn.rollback();
                }
                catch (Exception e) {
                    LOGGER.error( "Rollback failure", e );
                }
            }
        } catch (Throwable t) {
            if ( txn != null && txn.isActive() ) {
                try {
                    txn.rollback();
                }
                catch (Exception e) {
                    LOGGER.error( "Rollback failure", e );
                }
            }
            throw t;
        } finally {
            function.afterTransactionCompletion();
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
}

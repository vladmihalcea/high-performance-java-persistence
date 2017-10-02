package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import org.junit.Test;

import javax.persistence.*;

public class IdentityIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
    }

    @Test
    public void testIdentityIdentifierGenerator() {
        doInJPA(entityManager -> {
            for (int i = 0; i < 3; i++) {
                Post post = new Post();
                post.setTitle(
                    String.format("High-Performance Java Persistence, Part %d", i)
                );
                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testIdentityIdentifierGeneratorOutsideTransaction() {
        LOGGER.debug("testIdentityIdentifierGeneratorOutsideTransaction");
        EntityManager entityManager = null;
        EntityTransaction txn = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            for (int i = 0; i < 5; i++) {
                entityManager.persist(new Post());
            }
            txn = entityManager.getTransaction();
            txn.begin();
            txn.commit();
        } catch (RuntimeException e) {
            if ( txn != null && txn.isActive()) txn.rollback();
            throw e;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(
            strategy = GenerationType.IDENTITY
        )
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

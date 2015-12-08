package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.*;
import java.util.Properties;

public class IdentityIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

    @Test
    public void testIdentityIdentifierGenerator() {
        LOGGER.debug("testIdentityIdentifierGenerator");
        int batchSize = 2;
        doInJPA(entityManager -> {
            for (int i = 0; i < batchSize; i++) {
                entityManager.persist(new Post());
            }
            LOGGER.debug("Flush is triggered at commit-time");
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
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    }
}

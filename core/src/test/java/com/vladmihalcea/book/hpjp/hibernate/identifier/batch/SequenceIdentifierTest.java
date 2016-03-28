package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import org.junit.Test;

import javax.persistence.*;

public class SequenceIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

    @Test
    public void testSequenceCall() {
        LOGGER.debug("testSequenceGenerator");
        int batchSize = 2;
        doInJPA(entityManager -> {
            for (int i = 0; i < batchSize; i++) {
                entityManager.persist(new Post());
            }
            LOGGER.debug("Flush is triggered at commit-time");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy=GenerationType.SEQUENCE)
        private Long id;
    }

}

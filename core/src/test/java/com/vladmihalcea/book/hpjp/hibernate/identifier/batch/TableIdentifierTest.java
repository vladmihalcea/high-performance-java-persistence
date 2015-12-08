package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import org.junit.Test;

import javax.persistence.*;

public class TableIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

    @Test
    public void testTableIdentifierGenerator() {
        LOGGER.debug("testTableIdentifierGenerator");
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
        @GeneratedValue(strategy=GenerationType.TABLE)
        private Long id;
    }

}

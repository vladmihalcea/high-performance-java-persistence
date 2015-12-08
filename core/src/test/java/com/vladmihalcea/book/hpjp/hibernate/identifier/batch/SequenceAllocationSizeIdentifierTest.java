package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import javax.persistence.*;

public class SequenceAllocationSizeIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

    @Test
    public void testSequenceIdentifierGenerator() {
        LOGGER.debug("testSequenceIdentifierGenerator");
        doInJPA(entityManager -> {
            for (int i = 0; i < 2; i++) {
                entityManager.persist(new Post());
            }
            LOGGER.debug("Flush is triggered at commit-time");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GenericGenerator(name = "sequence", strategy = "sequence", parameters = {
            @org.hibernate.annotations.Parameter(name = "sequenceName", value = "sequence"),
            @org.hibernate.annotations.Parameter(name = "allocationSize", value = "1"),
        })
        @GeneratedValue(generator = "sequence", strategy=GenerationType.SEQUENCE)
        private Long id;
    }

}

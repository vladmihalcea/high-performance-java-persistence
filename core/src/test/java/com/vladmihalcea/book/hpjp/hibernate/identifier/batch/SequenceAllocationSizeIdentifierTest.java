package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
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
            for (int i = 0; i < 10; i++) {
                entityManager.persist(new Post());
            }
            LOGGER.debug("Flush is triggered at commit-time");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="pooledLo_seq")
        @GenericGenerator(name="pooledLo_seq", strategy="enhanced-sequence",
            parameters={
                @Parameter(name="sequence_name", value="pooledLo_sequence"),
                @Parameter(name="initial_value", value="1"),
                @Parameter(name="increment_size",value="2"),
                @Parameter(name="optimizer", value="pooled")
            })
        private Long id;
    }

}

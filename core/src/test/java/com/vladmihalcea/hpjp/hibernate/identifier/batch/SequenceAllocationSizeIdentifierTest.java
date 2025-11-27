package com.vladmihalcea.hpjp.hibernate.identifier.batch;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.junit.jupiter.api.Test;

import jakarta.persistence.*;

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
        @GeneratedValue(strategy= GenerationType.SEQUENCE, generator="pooled_seq")
        @GenericGenerator(
            name="pooled_seq",
            type= SequenceStyleGenerator.class,
            parameters={
                @Parameter(name="sequence_name", value="pooled_sequence"),
                @Parameter(name="initial_value", value="1"),
                @Parameter(name="increment_size",value="2"),
                @Parameter(name="optimizer", value="pooled")
            })
        private Long id;
    }

}

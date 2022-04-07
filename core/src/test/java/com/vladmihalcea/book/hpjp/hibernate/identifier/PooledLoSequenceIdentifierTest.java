package com.vladmihalcea.book.hpjp.hibernate.identifier;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class PooledLoSequenceIdentifierTest extends AbstractPooledSequenceIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class
        };
    }

    @Override
    protected Object newEntityInstance() {
        return new Post();
    }

    @Test
    public void testOptimizer() {
        insertSequences();
    }

    @Entity(name = "Post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pooled-lo")
        @GenericGenerator(
                name = "pooled-lo",
                strategy = "sequence",
                parameters = {
                    @Parameter(name = "sequence_name", value = "post_sequence"),
                    @Parameter(name = "initial_value", value = "1"),
                    @Parameter(name = "increment_size", value = "3"),
                    @Parameter(name = "optimizer", value = "pooled-lo")
                }
        )
        private Long id;
    }
}

package com.vladmihalcea.book.hpjp.hibernate.identifier;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PooledSequenceIdentifierTest extends AbstractPooledSequenceIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

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
        @GenericGenerator(name = "sequenceGenerator", strategy = "enhanced-sequence",
            parameters = {
                @org.hibernate.annotations.Parameter(name = "optimizer",
                    value = "pooled"
                ),
                @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                @org.hibernate.annotations.Parameter(name = "increment_size", value = "5")
            }
        )
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
        private Long id;
    }
}

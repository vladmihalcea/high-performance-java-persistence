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
                PooledSequenceIdentifier.class,
        };
    }

    protected Object newEntityInstance() {
        return new PooledSequenceIdentifier();
    }

    @Test
    public void testPooledOptimizerThrowsException() {
        try {
            insertSequences();
            fail("Expecting ConstraintViolationException!");
        } catch (Exception e) {
            assertEquals(ConstraintViolationException.class, e.getClass());
            LOGGER.error("Pooled optimizer threw", e);
        }
    }

    @Entity(name = "sequenceIdentifier")
    public static class PooledSequenceIdentifier {

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

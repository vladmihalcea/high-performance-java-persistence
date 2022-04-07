package com.vladmihalcea.book.hpjp.hibernate.identifier;

import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Properties;

public class PreferredPooledLoSequenceIdentifierTest extends AbstractPooledSequenceIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(AvailableSettings.PREFERRED_POOLED_OPTIMIZER, "pooled-lo");
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
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_sequence")
        @SequenceGenerator(
            name = "post_sequence",
            sequenceName = "post_sequence",
            allocationSize = 3
        )
        private Long id;
    }
}

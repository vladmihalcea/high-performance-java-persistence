package com.vladmihalcea.hpjp.hibernate.identifier;

import io.hypersistence.utils.hibernate.id.SequenceOptimizer;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

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
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @SequenceOptimizer(
            sequenceName = "post_sequence",
            initialValue = 1,
            incrementSize = 3,
            optimizer = "pooled-lo"
        )
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
}

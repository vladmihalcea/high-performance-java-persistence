package com.vladmihalcea.hpjp.hibernate.identifier;

import org.junit.Test;

import jakarta.persistence.*;

public class PooledDefaultSequenceIdentifierTest extends AbstractPooledSequenceIdentifierTest {

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
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pooled")
        @SequenceGenerator(
            name = "pooled",
            sequenceName = "post_sequence",
            allocationSize = 3
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

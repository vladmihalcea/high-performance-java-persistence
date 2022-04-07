package com.vladmihalcea.book.hpjp.hibernate.identifier;

import org.junit.Test;

import jakarta.persistence.*;

public class PooledSequenceIdentifierTest extends AbstractPooledSequenceIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
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

    @Test
    public void testPooledIdentifierGenerator() {
        doInJPA(entityManager -> {
            for (int i = 0; i < 4; i++) {
                Post post = new Post();
                post.setTitle(
                    String.format(
                        "High-Performance Java Persistence, Part %d",
                        i + 1
                    )
                );

                entityManager.persist(post);
            }
        });
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

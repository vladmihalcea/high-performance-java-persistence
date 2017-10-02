package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.hibernate.identifier.batch.AbstractBatchIdentifierTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;

public class SimpleSequenceIdentifierTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

    @Test
    public void testSequenceIdentifierGenerator() {
        doInJPA(entityManager -> {
            for (int i = 0; i < 3; i++) {
                Post post = new Post();
                post.setTitle(
                        String.format("High-Performance Java Persistence, Part %d", i + 1)
                );
                entityManager.persist(post);
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(
            strategy = GenerationType.SEQUENCE
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

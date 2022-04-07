package com.vladmihalcea.book.hpjp.hibernate.identifier.batch;

import org.junit.Test;

import jakarta.persistence.*;

public class TableIdentifierTest extends AbstractBatchIdentifierTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
        };
    }

    @Test
    public void testTableIdentifierGenerator() {
        LOGGER.debug("testTableIdentifierGenerator");
        int batchSize = 2;
        doInJPA(entityManager -> {
            for (int i = 1; i <= 5; i++) {
                entityManager.persist(
                    new Post().setTitle(
                        String.format(
                            "High-Performance Java Persistence, Part %d",
                            i
                        )
                    )
                );
            }
            LOGGER.debug("Flush is triggered at commit-time");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy=GenerationType.TABLE)
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }

}

package com.vladmihalcea.hpjp.hibernate.identifier;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.junit.Test;

public class HiloIdentifierTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testHiloIdentifierGenerator() {
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
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_sequence")
        @GenericGenerator(
            name = "post_sequence",
            strategy = "sequence",
            parameters = {
                @Parameter(name = "sequence_name", value = "post_sequence"),
                @Parameter(name = "initial_value", value = "1"),
                @Parameter(name = "increment_size", value = "3"),
                @Parameter(name = "optimizer", value = "hilo")
            }
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

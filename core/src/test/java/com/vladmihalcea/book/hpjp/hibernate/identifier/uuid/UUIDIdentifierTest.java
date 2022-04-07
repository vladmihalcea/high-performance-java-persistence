package com.vladmihalcea.book.hpjp.hibernate.identifier.uuid;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.GenericGenerator;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.UUID;

public class UUIDIdentifierTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void testUUIDIdentifierGenerator() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setTitle("High-Performance Java Persistence")
            );
            entityManager.flush();
            entityManager.merge(
                new Post()
                    .setTitle("High-Performance Java Persistence")
            );
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(generator = "uuid")
        @GenericGenerator(name = "uuid", strategy = "uuid")
        @Column(columnDefinition = "CHAR(32)")
        private String id;

        private String title;

        public String getId() {
            return id;
        }

        public Post setId(String id) {
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

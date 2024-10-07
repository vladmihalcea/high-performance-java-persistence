package com.vladmihalcea.hpjp.hibernate.identifier;

import com.vladmihalcea.hpjp.util.AbstractMySQLIntegrationTest;
import jakarta.persistence.*;
import org.junit.Test;

public class MySQLIdentityIdentifierTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            for (int i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setTitle(
                            String.format(
                                "High-Performance Java Persistence, Part %d", i
                            )
                        )
                );
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
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
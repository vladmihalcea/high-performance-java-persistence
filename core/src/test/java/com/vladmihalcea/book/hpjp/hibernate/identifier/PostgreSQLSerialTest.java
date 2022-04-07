package com.vladmihalcea.book.hpjp.hibernate.identifier;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class PostgreSQLSerialTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "5");
    }

    @Test
    public void testCurrentValue() {
        doInJPA(entityManager -> {
            Post post1 = new Post();
            post1.setTitle(
                "High-Performance Java Persistence, Part 1"
            );

            entityManager.persist(post1);

            Post post2 = new Post();
            post2.setTitle(
                "High-Performance Java Persistence, Part 2"
            );

            entityManager.persist(post2);

            entityManager.flush();
            assertEquals(
                2,
                (
                    (Number) entityManager
                    .createNativeQuery(
                        "select currval('post_id_seq')")
                    .getSingleResult()
                ).intValue()
            );
        });
    }

    @Test
    public void testBatch() {
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
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        private String title;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
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

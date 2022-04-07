package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLUpdateNullTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.id = 1L;
            post.externalId = 123L;
            post.title = "High-Performance Java Persistence";

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            int count = entityManager.createQuery(
                "update Post p " +
                "set p.externalId = :externalId, p.title = :title " +
                "where p.id = :id")
            .setParameter("externalId", null)
            .setParameter("title", null)
            .setParameter("id", 1L)
            .executeUpdate();
            assertEquals(1, count);

            Post post = entityManager.find(Post.class, 1L);
            assertNull(post.externalId);
            assertNull(post.title);
        });

        doInJPA(entityManager -> {
            int count = entityManager.createNativeQuery(
                "UPDATE post " +
                "SET externalId = :externalId, title = :title " +
                "WHERE id = :id")
            .unwrap(org.hibernate.query.NativeQuery.class)
            .setParameter("externalId", null, Long.class)
            .setParameter("title", null, String.class)
            .setParameter("id", 1L)
            .executeUpdate();
            assertEquals(1, count);

            Post post = entityManager.find(Post.class, 1L);
            assertNull(post.externalId);
            assertNull(post.title);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private Long externalId;

        @Column(columnDefinition = "text")
        private String title;
    }
}

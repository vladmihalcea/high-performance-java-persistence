package com.vladmihalcea.book.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EnumOrdinalTest extends AbstractMySQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
                Post post = new Post();
            post.setId(1L);
                post.setTitle("High-Performance Java Persistence");
            post.setStatus(PostStatus.PENDING);
                entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.createQuery("""
                select p
                from Post p
                where p.status = :status
                """, Post.class)
            .setParameter("status", PostStatus.PENDING)
            .getSingleResult();

            assertEquals("High-Performance Java Persistence", post.getTitle());
        });
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Enumerated(EnumType.ORDINAL)
        @Column(columnDefinition = "tinyint unsigned")
        private PostStatus status;

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

        public PostStatus getStatus() {
            return status;
        }

        public void setStatus(PostStatus status) {
            this.status = status;
        }
    }
}

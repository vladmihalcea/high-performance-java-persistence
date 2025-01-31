package com.vladmihalcea.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class EnumOrdinalOracleTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.ORACLE;
    }

    @Override
    protected void afterInit() {
        executeStatement("ALTER TABLE post DROP COLUMN status");
        executeStatement("ALTER TABLE post ADD status NUMBER(3)");
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setTitle("Check out my website")
                    .setStatus(PostStatus.REQUIRES_MODERATOR_INTERVENTION)
            );
        });

        doInJPA(entityManager -> {
            int postId = 50;

            for (int i = 0; i < 200; i++) {
                int rowCount = entityManager.createNativeQuery("""
                    INSERT INTO post (status, title, id)
                    VALUES (:status, :title, :id)
                    """)
                    .setParameter("status", i)
                    .setParameter("title", "Illegal Enum value")
                    .setParameter("id", postId++)
                    .executeUpdate();

                assertEquals(1, rowCount);
            }
        });

        LOGGER.info("");
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM,
        REQUIRES_MODERATOR_INTERVENTION
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Integer id;

        private String title;

        @Enumerated(EnumType.ORDINAL)
        private PostStatus status;

        public Integer getId() {
            return id;
        }

        public Post setId(Integer id) {
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

        public PostStatus getStatus() {
            return status;
        }

        public Post setStatus(PostStatus status) {
            this.status = status;
            return this;
        }
    }
}

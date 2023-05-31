package com.vladmihalcea.book.hpjp.hibernate.mapping.enums;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class EnumOrdinalDescriptionTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostStatusInfo.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    public void beforeInit() {
        executeStatement("DROP TYPE IF EXISTS post_status_info CASCADE");
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            PostStatusInfo pending = new PostStatusInfo();
            pending.setId(PostStatus.PENDING.ordinal());
            pending.setName(PostStatus.PENDING.name());
            pending.setDescription("Posts waiting to be approved by the admin");
            entityManager.persist(pending);

            PostStatusInfo approved = new PostStatusInfo();
            approved.setId(PostStatus.APPROVED.ordinal());
            approved.setName(PostStatus.APPROVED.name());
            approved.setDescription("Posts approved by the admin");
            entityManager.persist(approved);

            PostStatusInfo spam = new PostStatusInfo();
            spam.setId(PostStatus.SPAM.ordinal());
            spam.setName(PostStatus.SPAM.name());
            spam.setDescription("Posts rejected as spam");
            entityManager.persist(spam);

            PostStatusInfo moderated = new PostStatusInfo();
            moderated.setId(PostStatus.REQUIRES_MODERATOR_INTERVENTION.ordinal());
            moderated.setName(PostStatus.REQUIRES_MODERATOR_INTERVENTION.name());
            moderated.setDescription("Posts requires moderator intervention");
            entityManager.persist(moderated);
        });
    }

    @Test
    public void testPendingPost() {
        Post _post = doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");
            post.setStatus(PostStatus.PENDING);
            entityManager.persist(post);
            
            return post;
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, _post.getId());

            assertEquals(PostStatus.PENDING, post.getStatus());
            assertEquals("PENDING", post.getStatusInfo().getName());

            Tuple tuple = (Tuple) entityManager.createNativeQuery("""
                SELECT
                    p.id,
                    p.title,
                    p.status,
                    psi.name,
                    psi.description
                FROM post p
                INNER JOIN post_status_info psi ON p.status = psi.id
                WHERE p.id = :postId
                """, Tuple.class)
            .setParameter("postId", _post.getId())
            .getSingleResult();

            assertEquals("PENDING", tuple.get("name"));
            assertEquals("Posts waiting to be approved by the admin", tuple.get("description"));
        });
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

            try {
                entityManager.createNativeQuery("""
                    INSERT INTO post (status, title, id)
                    VALUES (:status, :title, :id)
                    """)
                .setParameter("status", 99)
                .setParameter("title", "Illegal Enum value")
                .setParameter("id", postId)
                .executeUpdate();

                fail("Should not allow us to insert an Enum value of 100!");
            } catch (PersistenceException e) {
                assertTrue(e.getCause() instanceof ConstraintViolationException);
            }
        });
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
        @Column(columnDefinition = "NUMERIC(2)")
        private PostStatus status;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(
            name = "status",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(
                name = "status_id"
            )
        )
        private PostStatusInfo statusInfo;

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

        public PostStatusInfo getStatusInfo() {
            return statusInfo;
        }
    }

    @Entity(name = "PostStatusInfo")
    @Table(name = "post_status_info")
    public static class PostStatusInfo {

        @Id
        @Column(columnDefinition = "NUMERIC(2)")
        private Integer id;

        @Column(length = 50)
        private String name;

        private String description;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}

package com.vladmihalcea.book.hpjp.hibernate.bulk;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class JPQLBulkUpdateDeleteTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testBulk() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setStatus(PostStatus.APPROVED)
            );

            entityManager.persist(
                new Post()
                    .setId(2L)
                    .setTitle("Spam title")
            );

            entityManager.persist(
                new Post()
                    .setId(3L)
                    .setMessage("Spam message")
            );

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setPost(entityManager.getReference(Post.class, 1L))
                    .setMessage("Spam comment")
            );
        });

        doInJPA(entityManager -> {
            assertEquals(2, flagPostSpam(entityManager));
            assertEquals(1, flagPostCommentSpam(entityManager));
        });

        doInJPA(entityManager -> {
            assertEquals(2,
                entityManager.createQuery("""
                    update Post
                    set updatedOn = :timestamp
                    where status = :status
                    """)
                .setParameter("timestamp", Timestamp.valueOf(LocalDateTime.now().minusDays(7)))
                .setParameter("status", PostStatus.SPAM)
                .executeUpdate()
            );

            assertEquals(1,
                entityManager.createQuery("""
                    update PostComment
                    set updatedOn = :timestamp
                    where status = :status
                    """)
                .setParameter("timestamp", Timestamp.valueOf(LocalDateTime.now().minusDays(3)))
                .setParameter("status", PostStatus.SPAM)
                .executeUpdate()
            );
        });

        doInJPA(entityManager -> {
            assertEquals(2, deletePostSpam(entityManager));
            assertEquals(1, deletePostCommentSpam(entityManager));
        });
    }

    public int flagPostSpam(EntityManager entityManager) {
        int updateCount = entityManager.createQuery("""
            update Post
            set 
                updatedOn = CURRENT_TIMESTAMP,
                status = :newStatus
            where 
                status = :oldStatus and
                (
                    lower(title) like :spamToken or
                    lower(message) like :spamToken
                )
            """)
        .setParameter("newStatus", PostStatus.SPAM)
        .setParameter("oldStatus", PostStatus.PENDING)
        .setParameter("spamToken", "%spam%")
        .executeUpdate();

        return updateCount;
    }

    public int flagPostCommentSpam(EntityManager entityManager) {
        int updateCount = entityManager.createQuery("""
            update PostComment
            set 
                updatedOn = CURRENT_TIMESTAMP,
                status = :newStatus
            where 
                status = :oldStatus and
                lower(message) like :spamToken
            """)
        .setParameter("newStatus", PostStatus.SPAM)
        .setParameter("oldStatus", PostStatus.PENDING)
        .setParameter("spamToken", "%spam%")
        .executeUpdate();

        return updateCount;
    }

    public int deletePostSpam(EntityManager entityManager) {
        int deleteCount = entityManager.createQuery("""
            delete from Post
            where 
                status = :status and
                updatedOn <= :validityThreshold
            """)
        .setParameter("status", PostStatus.SPAM)
        .setParameter(
            "validityThreshold",
            Timestamp.valueOf(
                LocalDateTime.now().minusDays(7)
            )
        )
        .executeUpdate();

        return deleteCount;
    }

    public int deletePostCommentSpam(EntityManager entityManager) {
        int deleteCount = entityManager.createQuery("""
            delete from PostComment
            where 
                status = :status and
                updatedOn <= :validityThreshold
            """)
        .setParameter("status", PostStatus.SPAM)
        .setParameter(
            "validityThreshold",
            Timestamp.valueOf(
                LocalDateTime.now().minusDays(3)
            )
        )
        .executeUpdate();

        return deleteCount;
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM
    }

    @MappedSuperclass
    public static abstract class PostModerate<T extends PostModerate> {

        @Enumerated(EnumType.ORDINAL)
        @Column(columnDefinition = "smallint")
        private PostStatus status = PostStatus.PENDING;

        @Column(name = "updated_on")
        private Date updatedOn = new Date();

        public PostStatus getStatus() {
            return status;
        }

        public T setStatus(PostStatus status) {
            this.status = status;
            return (T) this;
        }

        public Date getUpdatedOn() {
            return updatedOn;
        }

        public T setUpdatedOn(Date updatedOn) {
            this.updatedOn = updatedOn;
            return (T) this;
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post extends PostModerate<Post> {

        @Id
        private Long id;

        private String title;

        private String message;

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

        public String getMessage() {
            return message;
        }

        public Post setMessage(String message) {
            this.message = message;
            return this;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment extends PostModerate<PostComment> {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String message;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getMessage() {
            return message;
        }

        public PostComment setMessage(String message) {
            this.message = message;
            return this;
        }
    }
}

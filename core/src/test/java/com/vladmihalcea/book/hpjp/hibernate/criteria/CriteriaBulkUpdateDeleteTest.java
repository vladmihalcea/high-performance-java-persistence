package com.vladmihalcea.book.hpjp.hibernate.criteria;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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
public class CriteriaBulkUpdateDeleteTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
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
        });

        doInJPA(entityManager -> {
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
            assertEquals(2, flagSpam(entityManager, Post.class));
            assertEquals(1, flagSpam(entityManager, PostComment.class));
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
            assertEquals(2, deleteSpam(entityManager, Post.class));
            assertEquals(1, deleteSpam(entityManager, PostComment.class));
        });
    }

    public <T extends PostModerate> int flagSpam(EntityManager entityManager, Class<T> postModerateClass) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaUpdate<T> update = builder.createCriteriaUpdate(postModerateClass);

        Root<T> root = update.from(postModerateClass);

        Expression<Boolean> filterPredicate =
            builder.like(builder.lower(root.get("message")), "%spam%");

        if(Post.class.isAssignableFrom(postModerateClass)) {
            filterPredicate = builder.or(
                filterPredicate,
                builder.like(builder.lower(root.get("title")), "%spam%")
            );
        }

        update
        .set(root.get("status"), PostStatus.SPAM)
        .set(root.get("updatedOn"), new Date())
        .where(filterPredicate);

        return entityManager.createQuery(update).executeUpdate();
    }

    public <T extends PostModerate> int deleteSpam(EntityManager entityManager, Class<T> postModerateClass) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaDelete<T> delete = builder.createCriteriaDelete(postModerateClass);

        Root<T> root = delete.from(postModerateClass);

        int daysValidityThreshold = (Post.class.isAssignableFrom(postModerateClass)) ? 7 : 3;

        delete
        .where(
            builder.and(
                builder.equal(root.get("status"), PostStatus.SPAM),
                builder.lessThanOrEqualTo(root.get("updatedOn"), Timestamp.valueOf(LocalDateTime.now().minusDays(daysValidityThreshold)))
            )
        );

        return entityManager.createQuery(delete).executeUpdate();
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM
    }

    @MappedSuperclass
    public static abstract class PostModerate<T extends PostModerate> {

        @Enumerated(EnumType.ORDINAL)
        @Column(columnDefinition = "tinyint")
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

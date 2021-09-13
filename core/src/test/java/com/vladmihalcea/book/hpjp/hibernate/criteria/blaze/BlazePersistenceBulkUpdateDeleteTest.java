package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze;

import com.blazebit.persistence.*;
import com.blazebit.persistence.impl.builder.predicate.RestrictionBuilderImpl;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import javax.persistence.*;
import javax.persistence.criteria.*;
import javax.persistence.criteria.CriteriaBuilder;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BlazePersistenceBulkUpdateDeleteTest extends AbstractTest {

    private CriteriaBuilderFactory cbf;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected Database database() {
        return Database.MYSQL;
    }

    @Override
    protected EntityManagerFactory newEntityManagerFactory() {
        EntityManagerFactory entityManagerFactory = super.newEntityManagerFactory();
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        cbf = config.createCriteriaBuilderFactory(entityManagerFactory);
        return entityManagerFactory;
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
        UpdateCriteriaBuilder<T> updateCriteriaBuilder = cbf.update(
            entityManager,
            postModerateClass
        )
        .set("status", PostStatus.SPAM)
        .set("updatedOn", new Date());

        String spamToken = "%spam%";

        if(Post.class.isAssignableFrom(postModerateClass)) {
            updateCriteriaBuilder.whereOr()
                .where("lower(message)").like().value(spamToken).noEscape()
                .where("lower(title)").like().value(spamToken).noEscape()
            .endOr();
        } else {
            updateCriteriaBuilder
                .where("lower(message)").like().value(spamToken).noEscape();
        }

        return updateCriteriaBuilder.executeUpdate();
    }

    public <T extends PostModerate> int deleteSpam(EntityManager entityManager, Class<T> postModerateClass) {
        return cbf.delete(
            entityManager,
            postModerateClass
        )
        .where("status").eq(PostStatus.SPAM)
        .where("updatedOn").le(
            Timestamp.valueOf(
                LocalDateTime.now().minusDays(
                    (Post.class.isAssignableFrom(postModerateClass)) ? 7 : 3
                )
            )
        )
        .executeUpdate();
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

package com.vladmihalcea.hpjp.hibernate.association.chain;

import com.vladmihalcea.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import jakarta.persistence.*;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Vlad Mihalcea
 */
public class FetchManagedEntitiesUsingRecordsTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
            User.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.integrator_provider",
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(
                    List.of(
                        PostWithDetailsRecord.class
                    )
                )
            )
        );
    }

    @Test
    public void testLeftJoinFetchWithNonOptionalChild() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("First post");
            entityManager.persist(post);
            User user = new User().setFirstName("John").setLastName("Doe");
            entityManager.persist(user);

            entityManager.persist(
                new PostDetails().setCreatedBy(user).setPost(post)
            );
        });

        doInJPA(entityManager -> {
            PostDetails postDetails = entityManager.createQuery("""
                select pd
                from PostDetails pd
                join fetch pd.post p
                left join fetch pd.createdBy
                where pd.id = :postId
                """,
                PostDetails.class)
            .setParameter("postId", 1L)
            .getSingleResult();

            Post post = postDetails.getPost();
            assertEquals("First post", post.getTitle());
            assertEquals("John Doe", postDetails.getCreatedBy().getFullName());
        });
    }

    @Test
    public void testDetailsIsNull() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("First post")
            );
        });

        doInJPA(entityManager -> {
            PostWithUserRecord postWithUserRecord = entityManager.createQuery("""
                select new PostWithUserRecord(p, u)
                from Post p
                left join PostDetails pd on pd.id = p.id
                left join pd.createdBy u
                where p.id = :postId
                """,
                    PostWithUserRecord.class)
            .setParameter("postId", 1L)
            .getSingleResult();

            assertEquals("First post", postWithUserRecord.post().getTitle());
            assertNull(postWithUserRecord.user());

            //Check if the entities fetched via the PostWithDetailsRecord are managed
            Post post = postWithUserRecord.post();
            post.setTitle(post.getTitle() + " is awesome!");
            SQLStatementCountValidator.reset();
            entityManager.flush();
            SQLStatementCountValidator.assertUpdateCount(1);
        });
    }

    @Test
    public void testDetailsIsNotNull() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("First post");
            entityManager.persist(post);
            User user = new User().setFirstName("John").setLastName("Doe");
            entityManager.persist(user);

            entityManager.persist(
                new PostDetails().setCreatedBy(user).setPost(post)
            );
        });

        doInJPA(entityManager -> {
            PostWithUserRecord postWithUserRecord = entityManager.createQuery("""
                select new PostWithUserRecord(p, u)
                from Post p
                left join PostDetails pd on pd.id = p.id
                left join pd.createdBy u
                where p.id = :postId
                """,
                PostWithUserRecord.class)
            .setParameter("postId", 1L)
            .getSingleResult();

            assertEquals("First post", postWithUserRecord.post().getTitle());
            assertEquals("John Doe", postWithUserRecord.user().getFullName());

            //Check if the entities fetched via the PostWithDetailsRecord are managed
            User user = postWithUserRecord.user();
            user.setFirstName("Sir " + user.getFirstName());
            SQLStatementCountValidator.reset();
            entityManager.flush();
            SQLStatementCountValidator.assertUpdateCount(1);
        });
    }

    public record PostWithDetailsRecord(Post post, PostDetails details) {

    }

    public record PostWithUserRecord(Post post, User user) {

    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
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

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails {

        @Id
        private Long id;

        @Column(name = "created_on")
        private LocalDateTime createdOn = LocalDateTime.now();

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "created_by_user_id")
        private User createdBy;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Post post;

        public Long getId() {
            return id;
        }

        public PostDetails setId(Long id) {
            this.id = id;
            return this;
        }

        public LocalDateTime getCreatedOn() {
            return createdOn;
        }

        public PostDetails setCreatedOn(LocalDateTime createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public User getCreatedBy() {
            return createdBy;
        }

        public PostDetails setCreatedBy(User user) {
            this.createdBy = user;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostDetails setPost(Post post) {
            this.post = post;
            return this;
        }
    }

    @Entity(name = "User")
    @Table(name = "users")
    public static class User {

        @Id
        @GeneratedValue
        private Long id;

        @Column(name = "first_name")
        private String firstName;

        @Column(name = "last_name")
        private String lastName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public User setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public String getLastName() {
            return lastName;
        }

        public User setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public String getFullName() {
            return String.join(" ", firstName, lastName);
        }
    }
}

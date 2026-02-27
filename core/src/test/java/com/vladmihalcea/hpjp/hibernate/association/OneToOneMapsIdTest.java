package com.vladmihalcea.hpjp.hibernate.association;

import com.vladmihalcea.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import jakarta.persistence.*;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Vlad Mihalcea
 */
public class OneToOneMapsIdTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
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

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("First post")
            );
        });
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            PostWithDetailsRecord postWithDetailsRecord = entityManager.createQuery("""
                select new PostWithDetailsRecord(p, pd)
                from Post p
                left join PostDetails pd on pd.id = p.id
                where p.id = :postId
                """,
                PostWithDetailsRecord.class)
            .setParameter("postId", 1L)
            .getSingleResult();

            assertEquals("First post", postWithDetailsRecord.post().getTitle());
            assertNull(postWithDetailsRecord.details());
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            PostDetails details = new PostDetails().setCreatedBy("John Doe");
            details.setPost(post);
            entityManager.persist(details);
        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            PostDetails details = entityManager.find(PostDetails.class, post.getId());
            assertNotNull(details);

            details.setPost(null);
            SQLStatementCountValidator.reset();
            entityManager.flush();
            SQLStatementCountValidator.assertUpdateCount(0);
        });
    }

    @Test
    public void testFetchingBothEntitiesUsingRecord() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            PostDetails details = new PostDetails().setCreatedBy("John Doe");
            details.setPost(post);
            entityManager.persist(details);
        });

        doInJPA(entityManager -> {
            PostWithDetailsRecord postWithDetailsRecord = entityManager.createQuery("""
                select new PostWithDetailsRecord(p, pd)
                from Post p
                left join PostDetails pd on pd.id = p.id
                where p.id = :postId
                """,
                    PostWithDetailsRecord.class)
                .setParameter("postId", 1L)
                .getSingleResult();

            assertEquals("First post", postWithDetailsRecord.post().getTitle());
            assertEquals("John Doe", postWithDetailsRecord.details().getCreatedBy());

            //Check if the entities fetched via the PostWithDetailsRecord are managed
            Post post = postWithDetailsRecord.post();
            post.setTitle(post.getTitle() + " is awesome!");
            SQLStatementCountValidator.reset();
            entityManager.flush();
            SQLStatementCountValidator.assertUpdateCount(1);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            assertEquals("First post is awesome!", post.getTitle());
        });
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
        private Date createdOn;

        @Column(name = "created_by")
        private String createdBy;

        @OneToOne
        @MapsId
        private Post post;

        public Long getId() {
            return id;
        }

        public PostDetails setId(Long id) {
            this.id = id;
            return this;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public PostDetails setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public PostDetails setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
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

    public record PostWithDetailsRecord(Post post, PostDetails details) {

    }
}

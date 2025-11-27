package com.vladmihalcea.hpjp.hibernate.association;

import com.vladmihalcea.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.hpjp.util.AbstractTest;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import jakarta.persistence.*;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalOneToOneNPlusOneTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
            PostSummary.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.integrator_provider",
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(
                    List.of(
                        PostDTO.class
                    )
                )
            )
        );
    }

    @Override
    protected void afterInit() {
        doInJPA(entityManager -> {
            for (int i = 1; i <= 100; i++) {
                Post post = new Post().setTitle(String.format("Post nr. %d", i));
                post.setDetails(new PostDetails().setCreatedBy("Vlad Mihalcea"));

                entityManager.persist(post);
            }
        });
    }

    @Test
    @Disabled
    public void testNPlusOne() {
        SQLStatementCountValidator.reset();

        List<Post> posts = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p
                from Post p
                where p.title like 'Post nr.%'
                """, Post.class)
            .getResultList();
        });

        assertEquals(100, posts.size());
        SQLStatementCountValidator.assertSelectCount(1);
    }

    @Test
    public void testWithoutNPlusOne() {
        SQLStatementCountValidator.reset();

        List<PostSummary> posts = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select p
                from PostSummary p
                where p.title like 'Post nr.%'
                """, PostSummary.class)
            .getResultList();
        });

        assertEquals(100, posts.size());
        SQLStatementCountValidator.assertSelectCount(1);
    }

    @Test
    public void testFetchPostAndDetailsProjection() {
        doInJPA(entityManager -> {
            for (int i = 1; i <= 100; i++) {
                entityManager.persist(
                    new Post().setTitle(String.format("Post nr. %d", 100 + i))
                );
            }
        });

        SQLStatementCountValidator.reset();

        List<PostDTO> posts = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select new PostDTO(p.id, p.title, pd.createdOn, pd.createdBy)
                from PostSummary p
                left join PostDetails pd on p.id = pd.id
                where p.title like :titleToken
                """, PostDTO.class)
            .setParameter("titleToken", "Post nr.%")
            .getResultList();
        });

        assertEquals(200, posts.size());
        SQLStatementCountValidator.assertSelectCount(1);
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @OneToOne(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
        )
        private PostDetails details;

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

        public PostDetails getDetails() {
            return details;
        }

        public Post setDetails(PostDetails details) {
            if (details == null) {
                if (this.details != null) {
                    this.details.setPost(null);
                }
            }
            else {
                details.setPost(this);
            }
            this.details = details;
            return this;
        }
    }

    @Entity(name = "PostSummary")
    @Table(name = "post")
    public static class PostSummary {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        public Long getId() {
            return id;
        }

        public PostSummary setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public PostSummary setTitle(String title) {
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
        private Date createdOn = new Date();

        @Column(name = "created_by")
        private String createdBy;

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        @JoinColumn(name = "id")
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

    public static class PostDTO {

        private Long id;

        private String title;

        private Date createdOn;

        private String createdBy;

        public PostDTO(Long id, String title, Date createdOn, String createdBy) {
            this.id = id;
            this.title = title;
            this.createdOn = createdOn;
            this.createdBy = createdBy;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }
    }
}

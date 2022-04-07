package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    protected void afterInit() {
        doInJPA(entityManager -> {
            for (int i = 1; i <= 100; i++) {
                Post post = new Post(String.format("Post nr. %d", i));
                post.setDetails(new PostDetails("Excellent!"));

                entityManager.persist(post);
            }
        });
    }

    @Test
    @Ignore
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

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

        @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private PostDetails details;

        public Post() {}

        public Post(String title) {
            this.title = title;
        }

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

        public PostDetails getDetails() {
            return details;
        }

        public void setDetails(PostDetails details) {
            if (details == null) {
                if (this.details != null) {
                    this.details.setPost(null);
                }
            }
            else {
                details.setPost(this);
            }
            this.details = details;
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

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
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

        @OneToOne(fetch = FetchType.LAZY)
        @MapsId
        private Post post;

        public PostDetails() {}

        public PostDetails(String createdBy) {
            createdOn = new Date();
            this.createdBy = createdBy;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }
}

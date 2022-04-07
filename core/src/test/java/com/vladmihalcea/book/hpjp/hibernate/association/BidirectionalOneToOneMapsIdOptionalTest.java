package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class BidirectionalOneToOneMapsIdOptionalTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setTitle("High-Performance Java Persistence");

            PostDetails details = new PostDetails();
            details.setCreatedBy("Vlad Mihalcea");

            post.setDetails(details);

            entityManager.persist(post);
        });

        doInJPA(entityManager -> {
            LOGGER.info("Fetching Post");
            Post post = entityManager.find(Post.class, 1L);

            assertNotNull(post);

            post.setDetails(null);
        });
    }

    @Test
    public void testNPlusOne() {
        doInJPA(entityManager -> {
            Post post1 = new Post("First post");
            PostDetails details1 = new PostDetails("John Doe");
            post1.setDetails(details1);
            Post post2 = new Post("Second post");
            PostDetails details2 = new PostDetails("John Doe");
            post2.setDetails(details2);
            entityManager.persist(post1);
            entityManager.persist(post2);
        });
        doInJPA(entityManager -> {
            List<Post> posts = entityManager.createQuery(
                "select p " +
                "from Post p ", Post.class)
            .getResultList();
        });
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
            orphanRemoval = true,
            fetch = FetchType.LAZY
        )
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

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }
}

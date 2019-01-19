package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractMySQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

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

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");
            entityManager.persist(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            PostDetails details = new PostDetails("John Doe");
            details.setPost(post);
            entityManager.persist(details);
        });

        doInJPA(entityManager -> {
            Post secondPost = new Post("Second post");
            entityManager.persist(secondPost);

            Post post = entityManager.find(Post.class, 1L);
            PostDetails details = entityManager.find(PostDetails.class, post.getId());

            details.setPost(secondPost);
            PostDetails updatedDetails = entityManager.merge(details);

            assertEquals(updatedDetails.getId(), updatedDetails.getPost().getId()); // this will fail

        });

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            PostDetails details = entityManager.find(PostDetails.class, post.getId());
            assertNotNull(details);
            assertEquals(details.getId(), details.getPost().getId()); // this will pass

            entityManager.flush();
            details.setPost(null);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Long id;

        private String title;

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

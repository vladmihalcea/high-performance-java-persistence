package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Ignore;
import org.junit.Test;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class OneToOneIdTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostDetails.class,
        };
    }

    @Test
    @Ignore
    public void testLifecycle() {
        Post _post = doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("First post");

            PostDetails details = new PostDetails();
            details.setCreatedBy("John Doe");

            post.setDetails(details);
            entityManager.persist(post);

            return post;
        });

        _post.setTitle("Second post");
        _post.getDetails().setCreatedBy("Vlad Mihalcea");

        doInJPA(entityManager -> {
            Post post  = entityManager.merge(_post);
        });

        doInJPA(entityManager -> {
            PostDetails id = new PostDetails();
            id.setPost(_post);

            PostDetails details = entityManager.find(PostDetails.class, id);
            assertEquals("Vlad Mihalcea", details.getCreatedBy());
            assertEquals("Second post", details.getPost().getTitle());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post implements Serializable {

        @Id
        private Long id;

        private String title;

        @OneToOne(mappedBy = "post", cascade = CascadeType.ALL)
        private PostDetails details;

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
            this.details = details;
            this.details.setPost(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Post)) return false;
            return id != null && id.equals(((Post) o).getId());
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }

    @Entity(name = "PostDetails")
    @Table(name = "post_details")
    public static class PostDetails implements Serializable {

        @Id
        @OneToOne
        private Post post;

        @Column(name = "created_on")
        private Date createdOn = new Date();

        @Column(name = "created_by")
        private String createdBy;

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public Date getCreatedOn() {
            return createdOn;
        }

        public void setCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }
    }
}

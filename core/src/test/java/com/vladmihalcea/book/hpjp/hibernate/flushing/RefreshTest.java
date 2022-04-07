package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class RefreshTest extends AbstractTest {
    
    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
                Post.class,
                PostComment.class,
        };
    }
    @Override
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            entityManager.persist(post);

            assertNull(post.getCreatedOn());
            entityManager.flush();
            assertNotNull(post.getCreatedOn());
        });
    }

    @Test(expected = EntityNotFoundException.class)
    public void testChildEntity() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(0, post.getVersion());
            entityManager.createQuery("update versioned Post set title = 'n/a' where title is null").executeUpdate();

            assertEquals(0, post.getVersion());

            entityManager.refresh(post);
            assertEquals(1, post.getVersion());
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);

            PostComment comment = new PostComment();
            comment.setId(1L);
            comment.setReview("Great!");
            post.addComment(comment);

            entityManager.refresh(post);
        });
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNotNull(post.getCreatedOn());

            PostComment comment = new PostComment();
            comment.setId(1L);
            comment.setReview("Great!");
            post.addComment(comment);

            entityManager.refresh(post);
        });
    }

    @Test
    public void test() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            post.setTitle("JPA and Hibernate");
            entityManager.refresh(post);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on", columnDefinition = "timestamp default current_timestamp")
        @Generated(GenerationTime.INSERT)
        private String createdOn;

        @Version
        private int version;

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

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

        public String getCreatedOn() {
            return createdOn;
        }

        public long getVersion() {
            return version;
        }

        public List<PostComment> getComments() {
            return comments;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne
        private Post post;

        private String review;

        public PostComment() {
        }

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}

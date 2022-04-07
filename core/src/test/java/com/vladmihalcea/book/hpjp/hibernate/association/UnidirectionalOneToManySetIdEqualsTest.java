package com.vladmihalcea.book.hpjp.hibernate.association;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.junit.Test;

import com.vladmihalcea.book.hpjp.util.AbstractTest;

/**
 * @author Vlad Mihalcea
 */
public class UnidirectionalOneToManySetIdEqualsTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA(entityManager -> {
            Post post = new Post("First post");

            PostComment postComment1 = new PostComment("My first review");
            PostComment postComment2 = new PostComment("My second review");
            PostComment postComment3 = new PostComment("My third review");

            post.getComments().add(postComment1);
            post.getComments().add(postComment2);
            post.getComments().add(postComment3);

            entityManager.persist(post);
            entityManager.flush();

            LOGGER.info("Remove tail");
            post.getComments().remove(postComment1);
            entityManager.flush();
            LOGGER.info("Remove head");
            post.getComments().remove(postComment3);
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

        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        @JoinColumn(name = "post_id")
        private Set<PostComment> comments = new HashSet<>();

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

        public Set<PostComment> getComments() {
            return comments;
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        @GeneratedValue
        private Long id;

        private String review;

        public PostComment() {}

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PostComment )) return false;
            return id != null && id.equals(((PostComment) o).getId());
        }
        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
    }
}

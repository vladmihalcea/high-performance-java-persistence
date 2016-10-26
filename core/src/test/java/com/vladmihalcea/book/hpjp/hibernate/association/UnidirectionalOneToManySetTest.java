package com.vladmihalcea.book.hpjp.hibernate.association;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import javax.persistence.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Vlad Mihalcea
 */
public class UnidirectionalOneToManySetTest extends AbstractTest {

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

            post.getComments().add(new PostComment("My first review"));
            post.getComments().add(new PostComment("My second review"));
            post.getComments().add(new PostComment("My third review"));

            entityManager.persist(post);
            entityManager.flush();

            for(PostComment comment: new ArrayList<>(post.getComments())) {
                post.getComments().remove(comment);
            }
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

        private String slug;

        private String review;

        public PostComment() {
            byte[] bytes = new byte[8];
            ByteBuffer.wrap(bytes).putDouble(Math.random());
            slug = Base64.getEncoder().encodeToString(bytes);
        }

        public PostComment(String review) {
            this();
            this.review = review;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
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
            if (o == null || getClass() != o.getClass()) return false;
            PostComment comment = (PostComment) o;
            return Objects.equals(slug, comment.slug);
        }

        @Override
        public int hashCode() {
            return Objects.hash(slug);
        }
    }
}

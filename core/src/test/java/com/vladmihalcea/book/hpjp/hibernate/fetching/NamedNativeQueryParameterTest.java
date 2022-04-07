package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class NamedNativeQueryParameterTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }

    @Test
    public void testWithParameters() {
        doInJPA(entityManager -> {
            LongStream.range(0, 5).forEach(i -> {
                Post post = new Post(i);
                post.setTitle(String.format("Post nr. %d", i));

                LongStream.range(0, 5).forEach(j -> {
                    PostComment comment = new PostComment();
                    comment.setId((i * 5) + j);
                    comment.setReview(String.format("Good review nr. %d", comment.getId()));
                    comment.setPost(post);
                    entityManager.persist(comment);
                });
                entityManager.persist(post);
            });
        });
        doInJPA(entityManager -> {
            Session session = entityManager.unwrap(Session.class);
            List<PostComment> comments = session.getNamedQuery("findPostCommentsByPostTitle").setParameter("id", 0).list();
            assertTrue(!comments.isEmpty());
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @NamedNativeQuery(
        name = "findPostCommentsByPostTitle",
        query = """
            select c.*
            from post_comment c
            where c.id > :id
        """,
        resultClass = PostComment.class
    )
    public static class Post {

        @Id
        private Long id;

        private String title;

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

        public List<PostComment> getComments() {
            return comments;
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

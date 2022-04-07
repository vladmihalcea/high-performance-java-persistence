package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.junit.Test;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class BatchSizeCollectionTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }


    @Override
    public void init() {
        super.init();

        int commentsSize = 2;

        doInJPA(entityManager -> {
            LongStream.range(0, 5).forEach(i -> {
                Post post = new Post(i);
                post.setTitle(String.format("Post nr. %d", i));

                LongStream.range(0, commentsSize).forEach(j -> {
                    PostComment comment = new PostComment();
                    comment.setId((i * commentsSize) + j);
                    comment.setReview(String.format("Good review nr. %d", comment.getId()));
                    post.addComment(comment);

                });
                entityManager.persist(post);
            });
        });
    }

    @Test
    public void testFind() {
        doInJPA(entityManager -> {
            Post post1 = entityManager.find(Post.class, 1L);
            Post post2 = entityManager.find(Post.class, 2L);
            Post post3 = entityManager.find(Post.class, 3L);

            for (PostComment comment : post1.getComments()) {
                comment.getReview();
            }

            for (PostComment comment : post2.getComments()) {
                comment.getReview();
            }

            for (PostComment comment : post3.getComments()) {
                comment.getReview();
            }
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
        @BatchSize(size = 2)
        private List<PostComment> comments = new ArrayList<>();

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

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

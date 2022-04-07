package com.vladmihalcea.book.hpjp.hibernate.logging.validator;

import com.vladmihalcea.book.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class SQLStatementCountValidatorTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class,
            PostComment.class,
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post1 = new Post()
                .setId(1L)
                .setTitle("Post one");

            entityManager.persist(post1);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("Good")
                    .setPost(post1)
            );

            Post post2 = new Post()
                .setId(2L)
                .setTitle("Post two");

            entityManager.persist(post2);

            entityManager.persist(
                new PostComment()
                    .setId(2L)
                    .setReview("Excellent")
                    .setPost(post2)
            );
        });
    }

    @Test
    public void testNPlusOne() {
        doInJPA(entityManager -> {
            LOGGER.info("Detect N+1");
            SQLStatementCountValidator.reset();

            List<PostComment> comments = entityManager.createQuery("""
                select pc
                from PostComment pc
                """, PostComment.class)
            .getResultList();

            assertEquals(2, comments.size());

            SQLStatementCountValidator.assertSelectCount(1);
        });
    }

    @Test
    public void testJoinFetch() {
        doInJPA(entityManager -> {
            LOGGER.info("Join fetch to prevent N+1");
            SQLStatementCountValidator.reset();

            List<PostComment> postComments = entityManager.createQuery("""
                select pc 
                from PostComment pc 
                join fetch pc.post
                """, PostComment.class)
            .getResultList();

            for (PostComment postComment : postComments) {
                assertNotNull(postComment.getPost());
            }

            SQLStatementCountValidator.assertSelectCount(1);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }
    }
}

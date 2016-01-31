package com.vladmihalcea.book.hpjp.hibernate.logging.validator;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.sql.SQLStatementCountValidator;
import com.vladmihalcea.sql.exception.SQLSelectCountMismatchException;
import org.junit.Test;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * <code>SQLStatementCountValidatorTest</code> - SQLStatementCountValidator Test
 *
 * @author Vlad Mihalcea
 */
public class SQLStatementCountValidatorTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class,
            PostComment.class,
        };
    }

    @Test
    public void testValidate() {
        doInJPA(entityManager -> {
            Post post1 = new Post(1L);
            post1.setTitle("Post one");

            Post post2 = new Post(2L);
            post2.setTitle("Post two");

            PostComment comment1 = new PostComment();
            comment1.setId(1L);
            comment1.setReview("Good");

            PostComment comment2 = new PostComment();
            comment2.setId(2L);
            comment2.setReview("Excellent");

            post1.addComment(comment1);
            post2.addComment(comment2);
            entityManager.persist(post1);
            entityManager.persist(post2);
        });

        doInJPA(entityManager -> {
            LOGGER.info("Detect N+1");
            try {
                SQLStatementCountValidator.reset();
                List<PostComment> postComments = entityManager
                    .createQuery("select pc from PostComment pc", PostComment.class)
                    .getResultList();

                for(PostComment postComment : postComments) {
                    assertNotNull(postComment.getPost());
                }

                SQLStatementCountValidator.assertSelectCount(1);
            } catch (SQLSelectCountMismatchException e) {
                assertEquals(3, e.getRecorded());
            }
        });

        doInJPA(entityManager -> {
            LOGGER.info("Join fetch to prevent N+1");
            SQLStatementCountValidator.reset();
            List<PostComment> postComments = entityManager
                    .createQuery("select pc from PostComment pc join fetch pc.post", PostComment.class)
                    .getResultList();

            for(PostComment postComment : postComments) {
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

        public Post() {}

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

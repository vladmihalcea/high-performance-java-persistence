package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class EagerFetchingManyToOneFindEntityTest extends AbstractPostgreSQLIntegrationTest {

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
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle(String.format("Post nr. %d", 1));
            PostComment comment = new PostComment();
            comment.setId(1L);
            comment.setPost(post);
            comment.setReview("Excellent!");
            entityManager.persist(post);
            entityManager.persist(comment);
        });
    }

    @Test
    public void testFind() {
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            assertNotNull(comment);
        });
    }

    @Test
    public void testFindWithQuery() {
        doInJPA(entityManager -> {
            Long commentId =  1L;
            PostComment comment = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "where pc.id = :id", PostComment.class)
            .setParameter("id", commentId)
            .getSingleResult();
            assertNotNull(comment);
        });
    }

    @Test
    public void testFindWithQueryAndFetch() {
        doInJPA(entityManager -> {
            Long commentId =  1L;
            PostComment comment = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "left join fetch pc.post p " +
                "where pc.id = :id", PostComment.class)
            .setParameter("id", commentId)
            .getSingleResult();
            assertNotNull(comment);
        });
    }

    @Test
    public void testFindWithNamedEntityGraph() {
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L,
                Collections.singletonMap(
                    "javax.persistence.fetchgraph",
                    entityManager.getEntityGraph("PostComment.post")
                )
            );
            LOGGER.info("Fetch entity graph");
            assertNotNull(comment);
        });
    }

    @Test
    public void testNPlusOneDetection() {
        try {
            String review = "Excellent!";

            doInJPA(entityManager -> {
                LOGGER.info("Detect N+1");
                SQLStatementCountValidator.reset();
                List<PostComment> comments = entityManager.createQuery(
                    "select pc " +
                    "from PostComment pc " +
                    "join fetch pc.post " +
                    "where pc.review = :review", PostComment.class)
                .setParameter("review", review)
                .getResultList();
                SQLStatementCountValidator.assertSelectCount(1);
            });
        } catch (Exception expected) {
            LOGGER.error("Exception", expected);
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
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

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    @NamedEntityGraph(name = "PostComment.post", attributeNodes = {})
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

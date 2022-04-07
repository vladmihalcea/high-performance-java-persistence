package com.vladmihalcea.book.hpjp.hibernate.query.join.algorithm;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class NestedLoopsSqlTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }

    /**
     * post
     * ----
     * 
     * | id | title      |
     * |----|------------|
     * | 1  | Post no. 1 |
     * | 2  | Post no. 2 |
     * | .. | ..         |
     *
     * post_comment
     * -------------
     * 
     * | id | review        | post_id |
     * |----|---------------|---------|
     * | 1  | Comment no. 1 | 1       |
     * | 2  | Comment no. 2 | 1       |
     * | .. | ..            | ..      |
     */
    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            int postCount = 1000;
            int postCommentCount = 10;
            long postCommentId = 1;

            for (long postId = 1; postId <= postCount; postId++) {
                Post post = new Post()
                    .setId(postId)
                    .setTitle(String.format("Post no. %d", postId));
                entityManager.persist(post);

                for (int i = 0; i < postCommentCount; i++) {
                    entityManager.persist(
                        new PostComment()
                            .setId(postCommentId++)
                            .setReview(String.format("Comment no. %d", postCommentId))
                            .setPost(post)
                    );
                }
            }
        });
        executeStatement("CREATE INDEX IDX_post_id ON post (id)");
        executeStatement("CREATE INDEX IDX_post_comment_id ON post_comment (id)");
        executeStatement("CREATE INDEX IDX_post_comment_post_id ON post_comment (post_id)");
        executeStatement("VACUUM ANALYZE");
    }

    /**
     * Get all posts with their associated post_comments.
     *
     * EXPLAIN ANALYZE
     * SELECT
     *    p.id AS post_id,
     *    p.title AS post_title,
     *    pc.review  AS review
     * FROM post p
     * INNER JOIN post_comment pc ON pc.post_id = p.id
     * WHERE p.id BETWEEN 1 AND 10
     */
    @Test
    public void testInnerJoin() {
        doInJPA(entityManager -> {
            List<String> planLines = entityManager
            .unwrap(Session.class)
            .doReturningWork(connection -> selectColumnList(
                connection,
                """
                EXPLAIN ANALYZE
                SELECT
                   p.id AS post_id,
                   p.title AS post_title,
                   pc.review  AS review
                FROM post p
                INNER JOIN post_comment pc ON pc.post_id = p.id
                WHERE p.id BETWEEN 1 AND 10
                """,
                String.class)
            );

            assertTrue(planLines.size() > 1);

            LOGGER.info("Execution plan: {}{}",
                System.lineSeparator(),
                planLines.stream().collect(Collectors.joining(System.lineSeparator()))
            );
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

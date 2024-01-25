package com.vladmihalcea.hpjp.hibernate.query.cte;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class WithCTETest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    /**
     * post
     * ----
     *
     * | id | title    |
     * |----|----------|
     * | 1  | SQL:2016 |
     * | 2  | SQL:2011 |
     * | 3  | SQL:2008 |
     * | 4  | JPA 3.0  |
     * | 5  | JPA 2.2  |
     * | 6  | JPA 2.1  |
     * | 7  | JPA 2.0  |
     * | 8  | JPA 1.0  |
     *
     * post_comment
     * -------------
     *
     * | id | review                 | post_id |
     * |----|------------------------|---------|
     * | 1  | SQL:2016 is great!     | 1       |
     * | 2  | SQL:2016 is excellent! | 1       |
     * | 3  | SQL:2016 is awesome!   | 1       |
     * | 4  | SQL:2011 is great!     | 2       |
     * | 5  | SQL:2011 is excellent! | 2       |
     * | 6  | SQL:2008 is great!     | 3       |
     */
    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            short[] latestSQLStandards = {
                2016,
                2011,
                2008
            };

            String[] comments = {
                "great",
                "excellent",
                "awesome"
            };

            int post_id = 1;
            int comment_id = 1;

            for (int i = 0; i < latestSQLStandards.length; i++) {
                short sqlStandard = latestSQLStandards[i];
                Post post = new Post()
                    .setId(post_id++)
                    .setTitle(String.format("SQL:%d", sqlStandard));

                entityManager.persist(post);

                for (int j = 0; j < comments.length - i; j++) {
                    entityManager.persist(
                        new PostComment()
                            .setId(comment_id++)
                            .setReview(String.format("SQL:%d is %s!", sqlStandard, comments[j]))
                            .setPost(post)
                    );
                }
            }

            entityManager.persist(
                new Post()
                    .setId(post_id++)
                    .setTitle("JPA 3.0")
            );
            entityManager.persist(
                new Post()
                    .setId(post_id++)
                    .setTitle("JPA 2.2")
            );
            entityManager.persist(
                new Post()
                    .setId(post_id++)
                    .setTitle("JPA 2.1")
            );
            entityManager.persist(
                new Post()
                    .setId(post_id++)
                    .setTitle("JPA 2.0")
            );
            entityManager.persist(
                new Post()
                    .setId(post_id++)
                    .setTitle("JPA 1.0")
            );
        });
    }

    /**
     * Get the first two most-commented posts along with all their comments.
     *
     * WITH 
     * p_pc AS (
     *   SELECT
     *     p.id AS post_id,
     *     p.title AS post_title,
     *     pc.id AS comment_id,
     *     pc.review AS comment_review,
     *     COUNT(post_id) OVER(PARTITION BY post_id) AS comment_count
     *   FROM post p
     *   LEFT JOIN post_comment pc ON p.id = pc.post_id
     *   WHERE p.title LIKE 'SQL%'
     * ),
     * p_pc_r AS (
     *   SELECT
     *     post_id,
     *     post_title,
     *     comment_id,
     *     comment_review,
     *     DENSE_RANK() OVER (ORDER BY p_pc.comment_count DESC) AS ranking
     *   FROM p_pc
     * )
     * SELECT *
     * FROM p_pc_r
     * WHERE p_pc_r.ranking <= 2
     * ORDER BY post_id, comment_id
     *
     * | post_id | post_title | comment_id | comment_review         | ranking |
     * |---------|------------|------------|------------------------|---------|
     * | 1       | SQL:2016   | 1          | SQL:2016 is great!     | 1       |
     * | 1       | SQL:2016   | 2          | SQL:2016 is excellent! | 1       |
     * | 1       | SQL:2016   | 3          | SQL:2016 is awesome!   | 1       |
     * | 2       | SQL:2011   | 4          | SQL:2011 is great!     | 2       |
     * | 2       | SQL:2011   | 5          | SQL:2011 is excellent! | 2       |
     */
    @Test
    public void testWithCTESQL() {
        List<Tuple> tuples = doInJPA(entityManager -> {
            return entityManager.createNativeQuery("""
                WITH
                p_pc AS (
                  SELECT
                    p.id AS post_id,
                    p.title AS post_title,
                    pc.id AS comment_id,
                    pc.review AS comment_review,
                    COUNT(post_id) OVER(PARTITION BY post_id) AS comment_count
                  FROM post p
                  LEFT JOIN post_comment pc ON p.id = pc.post_id
                  WHERE p.title LIKE :title
                ),
                p_pc_r AS (
                  SELECT
                    post_id,
                    post_title,
                    comment_id,
                    comment_review,
                    DENSE_RANK() OVER (ORDER BY p_pc.comment_count DESC) AS ranking
                  FROM p_pc
                )
                SELECT *
                FROM p_pc_r
                WHERE p_pc_r.ranking <= :ranking
                ORDER BY post_id, comment_id
                """, Tuple.class)
            .setParameter("title", "SQL%")
            .setParameter("ranking", 2)
            .getResultList();
        });

        assertEquals(5, tuples.size());

        assertEquals(1, ((Number) tuples.get(0).get("post_id")).intValue());
        assertEquals("SQL:2016", tuples.get(0).get("post_title"));
        assertEquals(1, ((Number) tuples.get(0).get("comment_id")).intValue());
        assertEquals("SQL:2016 is great!", tuples.get(0).get("comment_review"));

        assertEquals(2, ((Number) tuples.get(3).get("post_id")).intValue());
        assertEquals("SQL:2011", tuples.get(3).get("post_title"));
        assertEquals(4, ((Number) tuples.get(3).get("comment_id")).intValue());
        assertEquals("SQL:2011 is great!", tuples.get(3).get("comment_review"));

        assertEquals(2, ((Number) tuples.get(4).get("post_id")).intValue());
        assertEquals("SQL:2011", tuples.get(4).get("post_title"));
        assertEquals(5, ((Number) tuples.get(4).get("comment_id")).intValue());
        assertEquals("SQL:2011 is excellent!", tuples.get(4).get("comment_review"));
    }

    @Test
    @Ignore("Still not working on Hibernate 6.3")
    public void testWithCTEJPQL() {
        List<Tuple> tuples = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                WITH p_pc AS (
                  SELECT
                    p.id AS post_id,
                    p.title AS post_title,
                    pc.id AS comment_id,
                    pc.review AS comment_review,
                    COUNT(p.id) OVER(PARTITION BY p.id) AS comment_count
                  FROM PostComment pc
                  JOIN pc.post p
                  WHERE p.title LIKE :title
                ),
                p_pc_r AS (
                  SELECT
                    post_id,
                    post_title,
                    comment_id,
                    comment_review,
                    DENSE_RANK() OVER (ORDER BY p_pc.comment_count DESC) AS ranking
                  FROM p_pc
                )
                SELECT *
                FROM p_pc_r
                WHERE p_pc_r.ranking <= :ranking
                ORDER BY post_id, comment_id
                """, Tuple.class)
            .setParameter("title", "SQL%")
            .setParameter("ranking", 2)
            .getResultList();
        });

        assertEquals(5, tuples.size());

        assertEquals(1, ((Number) tuples.get(0).get("post_id")).intValue());
        assertEquals("SQL:2016", tuples.get(0).get("post_title"));
        assertEquals(1, ((Number) tuples.get(0).get("comment_id")).intValue());
        assertEquals("SQL:2016 is great!", tuples.get(0).get("comment_review"));

        assertEquals(2, ((Number) tuples.get(3).get("post_id")).intValue());
        assertEquals("SQL:2011", tuples.get(3).get("post_title"));
        assertEquals(4, ((Number) tuples.get(3).get("comment_id")).intValue());
        assertEquals("SQL:2011 is great!", tuples.get(3).get("comment_review"));

        assertEquals(2, ((Number) tuples.get(4).get("post_id")).intValue());
        assertEquals("SQL:2011", tuples.get(4).get("post_title"));
        assertEquals(5, ((Number) tuples.get(4).get("comment_id")).intValue());
        assertEquals("SQL:2011 is excellent!", tuples.get(4).get("comment_review"));
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Integer id;

        private String title;

        public Integer getId() {
            return id;
        }

        public Post setId(Integer id) {
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
        private Integer id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public Integer getId() {
            return id;
        }

        public PostComment setId(Integer id) {
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

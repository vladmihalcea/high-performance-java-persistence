package com.vladmihalcea.book.hpjp.hibernate.query.join;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SQLInnerJoinTest extends AbstractTest {

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
     * | id | title     |
     * |----|-----------|
     * | 1  | Java      |
     * | 2  | Hibernate |
     * | 3  | JPA       |
     *
     * post_comment
     * -------------
     *
     * | id | review    | p.id    |
     * |----|-----------|---------|
     * | 1  | Good      | 1       |
     * | 2  | Excellent | 1       |
     * | 3  | Awesome   | 2       |
     */
    @Override
    public void afterInit() {
        doInJPA(entityManager -> {

            Post post1 = new Post()
                .setId(1L)
                .setTitle("Java");

            entityManager.persist(post1);

            Post post2 = new Post()
                .setId(2L)
                .setTitle("Hibernate");

            entityManager.persist(post2);

            Post post3 = new Post()
                .setId(3L)
                .setTitle("JPA");

            entityManager.persist(post3);

            entityManager.persist(
                new PostComment()
                    .setId(1L)
                    .setReview("Good")
                    .setPost(post1)
            );

            entityManager.persist(
                new PostComment()
                    .setId(2L)
                    .setReview("Excellent")
                    .setPost(post1)
            );

            entityManager.persist(
                new PostComment()
                    .setId(3L)
                    .setReview("Awesome")
                    .setPost(post2)
            );
        });
    }

    /**
     * Get all posts with their associated post_comments when the condition is always false.
     *
     * SELECT
     *    p.id AS "p.id",
     *    pc.id AS "pc.id"
     * FROM post p
     * INNER JOIN post_comment pc ON 1 = 0
     *
     * | p.id    | pc.id      |
     * |---------|------------|
     */
    @Test
    public void testInnerJoinOnFalse() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createNativeQuery("""
                SELECT
                     p.id AS "p.id",
                     pc.id AS "pc.id"
                FROM post p
                INNER JOIN post_comment pc ON 1 = 0
			    """, Tuple.class)
                .getResultList();

            assertEquals(0, tuples.size());
        });
    }

    /**
     * Get all posts with their associated post_comments when the condition is always true.
     *
     * SELECT
     *    p.id AS "p.id",
     *    pc.id AS "pc.id"
     * FROM post p
     * INNER JOIN post_comment pc ON 1 = 1
     * ORDER BY p.id, pc.id
     *
     * | p.id    | pc.id      |
     * |---------|------------|
     * | 1       | 1          |
     * | 1       | 2          |
     * | 1       | 3          |
     * | 2       | 1          |
     * | 2       | 2          |
     * | 2       | 3          |
     * | 3       | 1          |
     * | 3       | 2          |
     * | 3       | 3          |
     */
    @Test
    public void testInnerJoinOnTrue() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createNativeQuery("""
                SELECT
                     p.id AS "p.id",
                     pc.id AS "pc.id"
                FROM post p
                INNER JOIN post_comment pc ON 1 = 1
                ORDER BY p.id, pc.id
			    """, Tuple.class)
                .getResultList();

            assertEquals(3 * 3, tuples.size());

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    Tuple tuple = tuples.get(i * 3 + j);

                    assertEquals((i + 1), intValue(tuple.get("p.id")));
                    assertEquals((j  + 1), intValue(tuple.get("pc.id")));
                }
            }
        });
    }

    /**
     * Get all posts with their associated post_comments.
     *
     * SELECT
     *    p.id AS "p.id",
     *    pc.post_id AS "pc.post_id",
     *    pc.id AS "pc.id",
     *    p.title AS "p.title",
     *    pc.review  AS "pc.review"
     * FROM post p
     * INNER JOIN post_comment pc ON pc.post_id = p.id
     * ORDER BY p.id, pc.id
     *
     * | p.id    | pc.post_id | pc.id      | p.title    | pc.review |
     * |---------|------------|------------|------------|-----------|
     * | 1       | 1          | 1          | Java       | Good      |
     * | 1       | 1          | 2          | Java       | Excellent |
     * | 2       | 2          | 3          | Hibernate  | Awesome   |
     */
    @Test
    public void testInnerJoin() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createNativeQuery("""
                SELECT
                   p.id AS "p.id",
                   pc.post_id AS "pc.post_id",
                   pc.id AS "pc.id",
                   p.title AS "p.title",
                   pc.review  AS "pc.review"
                FROM post p
                INNER JOIN post_comment pc ON pc.post_id = p.id
                ORDER BY p.id, pc.id
			    """, Tuple.class)
                .getResultList();

            Tuple tuple1 = tuples.get(0);
            assertEquals(1L, longValue(tuple1.get("p.id")));
            assertEquals(1L, longValue(tuple1.get("pc.post_id")));
            assertEquals(1L, longValue(tuple1.get("pc.id")));
            assertEquals("Java", tuple1.get("p.title"));
            assertEquals("Good", tuple1.get("pc.review"));

            Tuple tuple2 = tuples.get(1);
            assertEquals(1L, longValue(tuple2.get("p.id")));
            assertEquals(1L, longValue(tuple2.get("pc.post_id")));
            assertEquals(2L, longValue(tuple2.get("pc.id")));
            assertEquals("Java", tuple2.get("p.title"));
            assertEquals("Excellent", tuple2.get("pc.review"));

            Tuple tuple3 = tuples.get(2);
            assertEquals(2L, longValue(tuple3.get("p.id")));
            assertEquals(2L, longValue(tuple3.get("pc.post_id")));
            assertEquals(3L, longValue(tuple3.get("pc.id")));
            assertEquals("Hibernate", tuple3.get("p.title"));
            assertEquals("Awesome", tuple3.get("pc.review"));
        });
    }

    /**
     * Get all posts with their associated post_comments.
     *
     * SELECT
     *    p.id AS "p.id",
     *    pc.post_id AS "pc.post_id",
     *    pc.id AS "pc.id",
     *    p.title AS "p.title",
     *    pc.review  AS "pc.review"
     * FROM post p, post_comment pc
     * WHERE pc.post_id = p.id
     * ORDER BY p.id, pc.id
     *
     * | p.id    | pc.post_id | pc.id      | p.title    | pc.review |
     * |---------|------------|------------|------------|-----------|
     * | 1       | 1          | 1          | Java       | Good      |
     * | 1       | 1          | 2          | Java       | Excellent |
     * | 2       | 2          | 3          | Hibernate  | Awesome   |
     */
    @Test
    public void testThetaStyleInnerJoin() {
        doInJPA(entityManager -> {
            List<Tuple> tuples = entityManager.createNativeQuery("""
                SELECT
                   p.id AS "p.id",
                   pc.post_id AS "pc.post_id",
                   pc.id AS "pc.id",
                   p.title AS "p.title",
                   pc.review  AS "pc.review"
                FROM post p, post_comment pc
                WHERE pc.post_id = p.id
			    """, Tuple.class)
                .getResultList();

            Tuple tuple1 = tuples.get(0);
            assertEquals(1L, longValue(tuple1.get("p.id")));
            assertEquals(1L, longValue(tuple1.get("pc.post_id")));
            assertEquals(1L, longValue(tuple1.get("pc.id")));
            assertEquals("Java", tuple1.get("p.title"));
            assertEquals("Good", tuple1.get("pc.review"));

            Tuple tuple2 = tuples.get(1);
            assertEquals(1L, longValue(tuple2.get("p.id")));
            assertEquals(1L, longValue(tuple2.get("pc.post_id")));
            assertEquals(2L, longValue(tuple2.get("pc.id")));
            assertEquals("Java", tuple2.get("p.title"));
            assertEquals("Excellent", tuple2.get("pc.review"));

            Tuple tuple3 = tuples.get(2);
            assertEquals(2L, longValue(tuple3.get("p.id")));
            assertEquals(2L, longValue(tuple3.get("pc.post_id")));
            assertEquals(3L, longValue(tuple3.get("pc.id")));
            assertEquals("Hibernate", tuple3.get("p.title"));
            assertEquals("Awesome", tuple3.get("pc.review"));
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

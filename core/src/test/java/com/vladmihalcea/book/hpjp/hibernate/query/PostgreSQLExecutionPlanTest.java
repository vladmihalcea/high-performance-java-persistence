package com.vladmihalcea.book.hpjp.hibernate.query;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLExecutionPlanTest extends AbstractTest {

    private static final int ENTITY_COUNT = 500;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
    }

    @Test
    public void testQueryExecutionPlan() {
        doInJPA(entityManager -> {
            for (int i = 0; i < ENTITY_COUNT; i++) {
                Post post = new Post(String.format("Post no. %d", i));

                post.addComment(new PostComment(String.format("Comment %d-1", i)));
                if (Math.random() < 0.1) {
                    post.addComment(new PostComment("Bingo"));
                }

                entityManager.persist(post);
            }
        });

        /*
        SELECT
            p.id
        FROM
            post p
        WHERE EXISTS (
            SELECT 1
            FROM
                post_comment pc
            WHERE
                pc.post_id = p.id AND
                pc.review = 'Bingo'
        )
        ORDER BY
            p.title
        LIMIT 10
        */

        List<String> executionPlanLines = doInJPA(entityManager -> {
            return entityManager.createNativeQuery(
                "EXPLAIN ANALYZE " +
                "SELECT " +
                "    p.id " +
                "FROM " +
                "    post p " +
                "WHERE EXISTS ( " +
                "    SELECT 1 " +
                "    FROM " +
                "        post_comment pc " +
                "    WHERE " +
                "        pc.post_id = p.id AND " +
                "        pc.review = 'Bingo' " +
                ") " +
                "ORDER BY " +
                "   p.title " +
                "LIMIT 10")
            .getResultList();
        });

        LOGGER.info("Execution plan: \n{}", executionPlanLines.stream().collect(Collectors.joining("\n")));
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        private String title;

        public Post() {}

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post", orphanRemoval = true)
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
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(foreignKey = @ForeignKey(name = "fk_post_id"))
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

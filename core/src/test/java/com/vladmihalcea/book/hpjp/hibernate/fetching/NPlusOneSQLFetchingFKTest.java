package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class NPlusOneSQLFetchingFKTest extends AbstractPostgreSQLIntegrationTest {

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

    @Test
    public void testNPlusOne() {

        doInJPA(entityManager -> {

            String[] reviews = new String[] {
                "Excellent book to understand Java Persistence",
                "Must-read for Java developers",
                "Five Stars",
                "A great reference book"
            };

            for (int i = 0; i < 4; i++) {
                long id = i + 1;

                Post post = new Post()
                    .setId(id)
                    .setTitle(String.format("High-Performance Java Persistence - Part %d", id));

                entityManager.persist(post);

                entityManager.persist(
                    new PostComment()
                        .setId(id)
                        .setPost(post)
                        .setReview(reviews[i])
                );
            }
        });

        doInJPA(entityManager -> {
            LOGGER.info("N+1 query problem");
            List<Tuple> comments = entityManager.createNativeQuery("""
                SELECT
                    pc.id AS id,
                    pc.review AS review,
                    pc.post_id AS postId
                FROM post_comment pc
                """, Tuple.class)
            .getResultList();

            for (Tuple comment : comments) {
                String review = (String) comment.get("review");
                Long postId = ((Number) comment.get("postId")).longValue();

                String postTitle = (String) entityManager.createNativeQuery("""
                    SELECT
                        p.title
                    FROM post p
                    WHERE p.id = :postId
                    """)
                .setParameter("postId", postId)
                .getSingleResult();

                LOGGER.info("The Post '{}' got this review '{}'", postTitle, review);
            }
        });

        doInJPA(entityManager -> {
            LOGGER.info("N+1 query problem fixed");

            List<Tuple> comments = entityManager.createNativeQuery("""
                SELECT
                    pc.id AS id,
                    pc.review AS review,
                    p.title AS postTitle
                FROM post_comment pc
                JOIN post p ON pc.post_id = p.id
                """, Tuple.class)
            .getResultList();

            for (Tuple comment : comments) {
                String review = (String) comment.get("review");
                String postTitle = (String) comment.get("postTitle");

                LOGGER.info("The Post '{}' got this review '{}'", postTitle, review);
            }
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

        @ManyToOne
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

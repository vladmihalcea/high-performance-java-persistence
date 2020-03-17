package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public class NPlusOneEagerFetchingManyToOneFindEntityTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Test
    public void testNPlusOne() {

        String review = "Excellent!";

        doInJPA(entityManager -> {

            for (long i = 1; i < 4; i++) {
                Post post = new Post()
                    .setId(i)
                    .setTitle(String.format("High-Performance Java Persistence, Part %d", i));

                entityManager.persist(post);

                entityManager.persist(
                    new PostComment()
                        .setId(i)
                        .setPost(post)
                        .setReview(review)
                );
            }
        });

        doInJPA(entityManager -> {
            LOGGER.info("N+1 query problem");
            List<PostComment> comments = entityManager
            .createQuery("""
                select pc
                from PostComment pc
                where pc.review = :review
                """, PostComment.class)
            .setParameter("review", review)
            .getResultList();

            LOGGER.info("Loaded {} comments", comments.size());
        });

        doInJPA(entityManager -> {
            LOGGER.info("N+1 query problem fixed");
            List<PostComment> comments = entityManager.createQuery("""
                select pc
                from PostComment pc
                join fetch pc.post p
                where pc.review = :review
                """, PostComment.class)
            .setParameter("review", review)
            .getResultList();
            LOGGER.info("Loaded {} comments", comments.size());
            for(PostComment comment : comments) {
                LOGGER.info("The post title is '{}'", comment.getPost().getTitle());
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

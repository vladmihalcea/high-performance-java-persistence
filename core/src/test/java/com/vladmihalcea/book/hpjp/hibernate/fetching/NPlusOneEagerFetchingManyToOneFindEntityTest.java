package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import jakarta.persistence.*;
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
            List<PostComment> comments = entityManager
            .createQuery("""
                select pc
                from PostComment pc
                """, PostComment.class)
            .getResultList();

            LOGGER.info("Loaded {} comments", comments.size());
        });

        doInJPA(entityManager -> {
            LOGGER.info("N+1 query problem fixed");
            List<PostComment> comments = entityManager.createQuery("""
                select pc
                from PostComment pc
                join fetch pc.post p
                """, PostComment.class)
            .getResultList();
            LOGGER.info("Loaded {} comments", comments.size());
            for(PostComment comment : comments) {
                LOGGER.info("The Post '{}' got this review '{}'", comment.getPost().getTitle(), comment.getReview());
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

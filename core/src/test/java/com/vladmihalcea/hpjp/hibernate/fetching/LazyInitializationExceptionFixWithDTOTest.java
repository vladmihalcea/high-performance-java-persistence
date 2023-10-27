package com.vladmihalcea.hpjp.hibernate.fetching;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import io.hypersistence.utils.hibernate.type.util.ClassImportIntegrator;
import jakarta.persistence.*;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @author Vlad Mihalcea
 */
public class LazyInitializationExceptionFixWithDTOTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put(
            "hibernate.integrator_provider",
            (IntegratorProvider) () -> Collections.singletonList(
                new ClassImportIntegrator(Collections.singletonList(PostCommentDTO.class))
            )
        );
    }

    @Test
    public void testNPlusOne() {

        String review = "Excellent!";

        doInJPA(entityManager -> {

            for (long i = 1; i < 4; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle(String.format("Post nr. %d", i));
                entityManager.persist(post);

                PostComment comment = new PostComment();
                comment.setId(i);
                comment.setPost(post);
                comment.setReview(review);
                entityManager.persist(comment);
            }
        });

        List<PostCommentDTO> comments = doInJPA(entityManager -> {
            return entityManager.createQuery("""
                select new
                   PostCommentDTO(
                       pc.id, pc.review, p.title
                   )
                from PostComment pc
                join pc.post p
                where pc.review = :review
                """, PostCommentDTO.class)
            .setParameter("review", review)
            .getResultList();
        });

        for(PostCommentDTO comment : comments) {
            LOGGER.info("The post title is '{}'", comment.getTitle());
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
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
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

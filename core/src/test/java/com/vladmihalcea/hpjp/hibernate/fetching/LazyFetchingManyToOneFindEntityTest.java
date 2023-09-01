package com.vladmihalcea.hpjp.hibernate.fetching;

import com.vladmihalcea.hpjp.hibernate.forum.PostComment_;
import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import jakarta.persistence.*;
import org.hibernate.LazyInitializationException;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class LazyFetchingManyToOneFindEntityTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class
        };
    }

    @Test
    public void testFind() {
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

        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);

            LOGGER.info("Loaded comment entity");
            LOGGER.info("The post title is '{}'", comment.getPost().getTitle());
            assertNotNull(comment);
        });

        doInJPA(entityManager -> {
            LOGGER.info("Using custom entity graph");

            EntityGraph<PostComment> postEntityGraph = entityManager.createEntityGraph(
                PostComment.class);
            postEntityGraph.addAttributeNodes("post");

            PostComment comment = entityManager.find(PostComment.class, 1L,
                Collections.singletonMap("jakarta.persistence.fetchgraph", postEntityGraph)
            );
            LOGGER.info("Fetch entity graph");
            assertNotNull(comment);
        });

        doInJPA(entityManager -> {
            LOGGER.info("Using JPQL");

            PostComment comment = entityManager.createQuery("""
                select pc
                from PostComment pc
                join fetch pc.post p
                where pc.id = :id
                """, PostComment.class)
            .setParameter("id", 1L)
            .getSingleResult();
            assertNotNull(comment);
        });
    }

    @Test
    public void testNPlusOne() {

        String review = "Excellent!";

        doInJPA(entityManager -> {

            for (long i = 1; i <= 3; i++) {
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

        doInJPA(entityManager -> {
            LOGGER.info("N+1 query problem");
            List<PostComment> comments = entityManager.createQuery("""
                select pc
                from PostComment pc
                where pc.review = :review
                """, PostComment.class)
            .setParameter("review", review)
            .getResultList();

            LOGGER.info("Loaded {} comments", comments.size());

            for(PostComment comment : comments) {
                LOGGER.info("The post title is '{}'", comment.getPost().getTitle());
            }
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

    @Test(expected = LazyInitializationException.class)
    public void testSessionIsClosed() {
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

        PostComment comment = null;

        EntityManager entityManager = null;
        EntityTransaction transaction = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            transaction = entityManager.getTransaction();
            transaction.begin();

            comment = entityManager.find(PostComment.class, 1L);

            transaction.commit();
        } catch (Throwable e) {
            if ( transaction != null && transaction.isActive())
                transaction.rollback();
            throw e;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

        LOGGER.info("The post title is '{}'", comment.getPost().getTitle());
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

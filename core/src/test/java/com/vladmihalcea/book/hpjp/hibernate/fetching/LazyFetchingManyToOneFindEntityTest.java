package com.vladmihalcea.book.hpjp.hibernate.fetching;

import com.vladmihalcea.book.hpjp.hibernate.forum.*;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.hibernate.LazyInitializationException;
import org.junit.Test;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
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
            PostComment.class,
            PostDetails.class,
            Tag.class
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
            postEntityGraph.addAttributeNodes(PostComment_.post);

            PostComment comment = entityManager.find(PostComment.class, 1L,
                Collections.singletonMap("javax.persistence.fetchgraph", postEntityGraph)
            );
            LOGGER.info("Fetch entity graph");
            assertNotNull(comment);
        });

        doInJPA(entityManager -> {
            LOGGER.info("Using JPQL");

            PostComment comment = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "join fetch pc.post p " +
                "where pc.id = :id", PostComment.class)
            .setParameter("id", 1L)
            .getSingleResult();
            assertNotNull(comment);
        });
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

        doInJPA(entityManager -> {
            LOGGER.info("N+1 query problem");
            List<PostComment> comments = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "where pc.review = :review", PostComment.class)
            .setParameter("review", review)
            .getResultList();
            LOGGER.info("Loaded {} comments", comments.size());
            for(PostComment comment : comments) {
                LOGGER.info("The post title is '{}'", comment.getPost().getTitle());
            }
        });

        doInJPA(entityManager -> {
            LOGGER.info("N+1 query problem fixed");
            List<PostComment> comments = entityManager.createQuery(
                "select pc " +
                "from PostComment pc " +
                "join fetch pc.post p " +
                "where pc.review = :review", PostComment.class)
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
}

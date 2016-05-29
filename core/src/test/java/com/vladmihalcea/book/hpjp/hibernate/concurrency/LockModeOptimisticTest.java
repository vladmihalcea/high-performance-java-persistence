package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import org.junit.Test;

import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * LockModeOptimisticTest - Test to check LockMode.OPTIMISTIC
 *
 * @author Vlad Mihalcea
 */
public class LockModeOptimisticTest extends AbstractLockModeOptimisticTest {

    @Test
    public void testImplicitOptimisticLocking() {

        doInJPA(entityManager -> {
            LOGGER.info("Alice loads the Post entity");
            Post post = entityManager.find(Post.class, 1L);

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    LOGGER.info("Bob loads the Post entity and modifies it");
                    Post _post = _entityManager.find(Post.class, 1L);
                    _post.setBody("Chapter 17 summary");
                });
            });

            LOGGER.info("Alice adds a PostComment to the previous Post entity version");
            PostComment comment = new PostComment();
            comment.setId(1L);
            comment.setReview("Chapter 16 is about Caching.");
            comment.setPost(post);
            entityManager.persist(comment);
        });
    }

    @Test
    public void testExplicitOptimisticLocking() {

        try {
            doInJPA(entityManager -> {
                LOGGER.info("Alice loads the Post entity");
                Post post = entityManager.find(Post.class, 1L);

                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        LOGGER.info("Bob loads the Post entity and modifies it");
                        Post _post = _entityManager.find(Post.class, 1L);
                        _post.setBody("Chapter 17 summary");
                    });
                });

                entityManager.lock(post, LockModeType.OPTIMISTIC);

                LOGGER.info("Alice adds a PostComment to the previous Post entity version");
                PostComment comment = new PostComment();
                comment.setId(1L);
                comment.setReview("Chapter 16 is about Caching.");
                comment.setPost(post);
                entityManager.persist(comment);
            });
            fail("It should have thrown OptimisticEntityLockException!");
        } catch (RollbackException expected) {
            assertEquals(OptimisticLockException.class, expected.getCause().getClass());
            LOGGER.info("Failure: ", expected);
        }
    }
}

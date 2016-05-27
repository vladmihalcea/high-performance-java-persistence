package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import org.junit.Test;

import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;

import static org.junit.Assert.*;

/**
 * LockModeOptimisticTest - Test to check LockMode.OPTIMISTIC
 *
 * @author Vlad Mihalcea
 */
public class LockModeOptimisticTest extends AbstractLockModeOptimisticTest {

    @Test
    public void testExplicitOptimisticLocking() {

        try {
            doInJPA(entityManager -> {
                final Post post = entityManager.find(Post.class, 1L, LockModeType.OPTIMISTIC);

                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        Post _post = _entityManager.find(Post.class, 1L);
                        assertNotSame(post, _post);
                        _post.setTitle("High-performance JDBC");
                    });
                });

                PostComment comment = new PostComment();
                comment.setId(1L);
                comment.setReview("Good one.");
                comment.setPost(post);
            });
            fail("It should have thrown OptimisticEntityLockException!");
        } catch (Exception expected) {
            assertEquals(OptimisticLockException.class, expected.getCause().getClass());
            LOGGER.info("Failure: ", expected);
        }
    }
}

package com.vladmihalcea.hpjp.spring.data.lock;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.lock.config.SpringDataJPALockConfiguration;
import com.vladmihalcea.hpjp.spring.data.lock.domain.Post;
import com.vladmihalcea.hpjp.spring.data.lock.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.lock.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.lock.repository.PostRepository;
import jakarta.persistence.LockModeType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionCallback;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPALockConfiguration.class)
public class SpringDataJPALockTest extends AbstractSpringTest {

    public static final int POST_COMMENT_COUNT = 5;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class,
        };
    }

    @Test
    public void test() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post firstPost = postRepository.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setSlug("high-performance-java-persistence")
            );

            postRepository.persist(
                new Post()
                    .setId(2L)
                    .setTitle("Hypersistence Optimizer")
                    .setSlug("hypersistence-optimizer")
            );

            long commentId = 0;

            for (long i = 0; i < POST_COMMENT_COUNT; i++) {
                entityManager.persist(
                    new PostComment()
                        .setId(++commentId)
                        .setReview(
                            String.format("The %d chapter is amazing!", commentId)
                        )
                    .setPost(firstPost)
                );
            }

            return null;
        });

        Post post = postRepository.findBySlug("high-performance-java-persistence");
        assertNotNull(post);

        transactionTemplate.execute(transactionStatus -> {
            Post postWithSharedLock = postRepository.lockById(1L, LockModeType.PESSIMISTIC_READ);

            Post postWithExclusiveLock = postRepository.lockById(2L, LockModeType.PESSIMISTIC_WRITE);

            List<PostComment> commentWithLock = postCommentRepository.lockAllByPostId(1L);
            assertEquals(POST_COMMENT_COUNT, commentWithLock.size());

            return null;
        });
    }
}


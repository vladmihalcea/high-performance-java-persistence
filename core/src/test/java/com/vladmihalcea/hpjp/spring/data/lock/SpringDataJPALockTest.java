package com.vladmihalcea.hpjp.spring.data.lock;

import com.vladmihalcea.hpjp.spring.data.lock.config.SpringDataJPALockConfiguration;
import com.vladmihalcea.hpjp.spring.data.lock.domain.Post;
import com.vladmihalcea.hpjp.spring.data.lock.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.lock.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.lock.repository.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPALockConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPALockTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public static final int POST_COMMENT_COUNT = 5;

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

        transactionTemplate.execute(transactionStatus -> {
            Post postWithSharedLock = postRepository.lockById(1L, LockModeType.PESSIMISTIC_READ);

            Post postWithExclusiveLock = postRepository.lockById(2L, LockModeType.PESSIMISTIC_WRITE);

            List<PostComment> commentWithLock = postCommentRepository.lockAllByPostId(1L);
            assertEquals(POST_COMMENT_COUNT, commentWithLock.size());

            return null;
        });
    }
}


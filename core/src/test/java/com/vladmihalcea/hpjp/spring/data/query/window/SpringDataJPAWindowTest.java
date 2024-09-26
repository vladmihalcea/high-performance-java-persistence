package com.vladmihalcea.hpjp.spring.data.query.window;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.query.window.config.SpringDataJPAWindowConfiguration;
import com.vladmihalcea.hpjp.spring.data.query.window.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.window.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.window.domain.PostComment_;
import com.vladmihalcea.hpjp.spring.data.query.window.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.query.window.repository.PostRepository;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.WindowIterator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.time.LocalDateTime;


/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAWindowConfiguration.class)
public class SpringDataJPAWindowTest extends AbstractSpringTest {

    public static final int POST_COMMENT_COUNT = 30;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                LocalDateTime timestamp = LocalDateTime.of(
                    2024, 9, 26, 4, 0, 0, 0
                );

                long commentId = 1;

                Post post = new Post()
                    .setId(1L)
                    .setTitle("Post nr. 1");

                for (long i = 1; i <= POST_COMMENT_COUNT; i++) {
                    post.addComment(
                        new PostComment()
                            .setId(commentId++)
                            .setReview(String.format("Awesome post %d", i))
                            .setStatus(PostComment.Status.PENDING)
                            .setCreatedOn(timestamp.plusHours(commentId))
                            .setVotes((int) (i % 7))
                    );
                }

                entityManager.persist(post);

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void testWindowIterator() {
        Post post = postRepository.getReferenceById(1L);

        int pageSize = 10;

        WindowIterator<PostComment> commentWindowIterator = WindowIterator.of(
            position -> postCommentRepository.findByPost(
                post,
                PageRequest.of(
                    0,
                    pageSize,
                    Sort.by(
                        Sort.Order.desc(PostComment_.CREATED_ON),
                        Sort.Order.desc(PostComment_.ID)
                    )
                ),
                position
            )
        ).startingAt(ScrollPosition.keyset());

        commentWindowIterator.forEachRemaining(
            comment -> LOGGER.info(
                "Post comment {} created at {}",
                comment.getReview(),
                comment.getCreatedOn()
            )
        );
    }
}


package com.vladmihalcea.hpjp.spring.data.query.hint;

import com.vladmihalcea.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.query.hint.config.SpringDataJPAQueryHintConfiguration;
import com.vladmihalcea.hpjp.spring.data.query.hint.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.hint.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.hint.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.query.hint.service.ForumService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAQueryHintConfiguration.class)
public class SpringDataJPAQueryHintTest extends AbstractSpringTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ForumService forumService;

    @Autowired
    private DataSource dataSource;

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

                int COMMENT_COUNT = 10;

                LocalDateTime timestamp = LocalDateTime.of(
                    2023, 3, 22, 12, 0, 0, 0
                );

                LongStream.rangeClosed(1, 10).forEach(postId -> {
                    Post post = new Post()
                        .setId(postId)
                        .setTitle(
                            String.format("High-Performance Java Persistence - Chapter %d",
                                postId)
                        )
                        .setCreatedOn(timestamp.plusMinutes(postId));

                    LongStream.rangeClosed(1, COMMENT_COUNT)
                        .forEach(commentOffset -> {
                            long commentId = ((postId - 1) * COMMENT_COUNT) + commentOffset;

                            post.addComment(
                                new PostComment()
                                    .setId(commentId)
                                    .setReview(
                                        String.format("Comment nr. %d - A must-read!", commentId)
                                    )
                                    .setCreatedOn(timestamp.plusMinutes(commentId))
                            );

                        });

                    postRepository.persist(post);
                });

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void testFindTopNWithCommentsByTitle() {
        SQLStatementCountValidator.reset();
        List<Post> posts = forumService.findAllByIdWithComments(List.of(1L, 2L, 3L));

        assertEquals(3, posts.size());
        SQLStatementCountValidator.assertSelectCount(1);
        SQLStatementCountValidator.assertUpdateCount(0);
    }
}


package com.vladmihalcea.hpjp.spring.data.query.fetch;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.query.fetch.config.SpringDataJPAJoinFetchPaginationConfiguration;
import com.vladmihalcea.hpjp.spring.data.query.fetch.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.fetch.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.fetch.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.query.fetch.service.ForumService;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAJoinFetchPaginationConfiguration.class)
public class SpringDataJPAJoinFetchPaginationTest extends AbstractSpringTest {

    public static final int POST_COUNT = 1_000;
    public static final int COMMENT_COUNT = 10;

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
                LocalDateTime timestamp = LocalDate.now().atStartOfDay().plusHours(12);

                LongStream.rangeClosed(1, POST_COUNT).forEach(postId -> {
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

        executeStatement(dataSource, "CREATE INDEX IF NOT EXISTS idx_post_created_on ON post (created_on)");
        executeStatement(dataSource, "CREATE INDEX IF NOT EXISTS idx_post_comment_post_id ON post_comment (post_id)");
        executeStatement(dataSource, "ANALYZE VERBOSE");
    }

    @Test
    public void testFindAllByTitleTopN() {
        int maxCount = 25;

        Page<Post> posts = postRepository.findAllByTitleLike(
            "High-Performance Java Persistence %",
            PageRequest.of(0, maxCount, Sort.by("createdOn"))
        );

        assertEquals(maxCount, posts.getSize());
    }

    @Test
    public void testFindAllByTitleNextN() {
        int maxCount = 25;

        Page<Post> posts = postRepository.findAllByTitleLike(
            "High-Performance Java Persistence %",
            PageRequest.of(1, maxCount, Sort.by("createdOn"))
        );

        assertEquals(maxCount, posts.getSize());
    }

    @Test
    public void testFindAllByTitleTopNWithNativeQuery() {
        int maxCount = 25;

        Page<Post> posts = postRepository.findAllByTitleLikeNativeQuery(
            "High-Performance Java Persistence %",
            PageRequest.of(0, maxCount)
        );

        assertEquals(maxCount, posts.getSize());
    }

    @Test
    public void testFindAllByTitleNextNWithNativeQuery() {
        int maxCount = 25;

        Page<Post> posts = postRepository.findAllByTitleLikeNativeQuery(
            "High-Performance Java Persistence %",
            PageRequest.of(1, maxCount)
        );

        assertEquals(maxCount, posts.getSize());
    }

    @Test
    public void testFindAllWithCommentsByTitleAntiPattern() {

        int maxCount = 25;

        try {
            Page<Post> posts = postRepository.findAllByTitleWithCommentsAntiPattern(
                "High-Performance Java Persistence %",
                PageRequest.of(0, maxCount, Sort.by("createdOn", "id"))
            );

            assertEquals(maxCount, posts.getSize());
        } catch (Exception e) {
            LOGGER.error("In-memory pagination", e);

            assertTrue(ExceptionUtil.rootCause(e).getMessage().startsWith("firstResult/maxResults specified with collection fetch"));
        }
    }

    @Test
    public void testFindTopNWithCommentsByTitle() {

        int maxCount = 25;

        List<Post> posts = postRepository.findFirstByTitleWithCommentsByTitle(
            "High-Performance Java Persistence %",
            maxCount
        );

        assertEquals(maxCount, posts.size());
    }

    @Test
    public void testFindWithCommentsByTitleWithTwoQueries() {

        int maxCount = 25;

        List<Post> posts = forumService.findAllPostsByTitleWithComments(
            "High-Performance Java Persistence %",
            PageRequest.of(0, maxCount, Sort.by("createdOn"))
        );

        assertEquals(maxCount, posts.size());
    }

    protected void executeStatement(DataSource dataSource, String sql) {
        Boolean initialAutoCommit = null;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            try {
                initialAutoCommit = connection.getAutoCommit();
                if (!initialAutoCommit) {
                    connection.setAutoCommit(true);
                }
                statement.executeUpdate(sql);
            } finally {
                if(initialAutoCommit != null) {
                    connection.setAutoCommit(initialAutoCommit);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Statement failed", e);
        }
    }
}


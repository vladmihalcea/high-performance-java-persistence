package com.vladmihalcea.book.hpjp.spring.data.query.fetch;

import com.vladmihalcea.book.hpjp.spring.data.query.fetch.config.SpringDataJPAJoinFetchPaginationConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.query.fetch.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.query.fetch.domain.PostComment;
import com.vladmihalcea.book.hpjp.spring.data.query.fetch.repository.PostRepository;
import com.vladmihalcea.book.hpjp.spring.data.query.fetch.service.ForumService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAJoinFetchPaginationConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAJoinFetchPaginationTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ForumService forumService;

    @Autowired
    private DataSource dataSource;

    @Before
    public void init() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {

                int COMMENT_COUNT = 10;

                LocalDateTime timestamp = LocalDateTime.of(
                    2023, 3, 22, 12, 0, 0, 0
                );

                LongStream.rangeClosed(1, 1_000).forEach(postId -> {
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
        executeStatement(dataSource, "VACUUM FULL");
    }

    @Test
    public void testFindAllWithCommentsByTitleAntiPattern() {

        int maxCount = 25;

        Page<Post> posts = postRepository.findAllByTitleWithCommentsAntiPattern(
            "High-Performance Java Persistence %",
            PageRequest.of(0, maxCount, Sort.by("createdOn"))
        );

        assertEquals(maxCount, posts.getSize());
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


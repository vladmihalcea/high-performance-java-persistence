package com.vladmihalcea.hpjp.spring.data.query.fetch;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.query.fetch.config.SpringDataJPAJoinFetchPaginationConfiguration;
import com.vladmihalcea.hpjp.spring.data.query.fetch.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.fetch.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.fetch.domain.Post_;
import com.vladmihalcea.hpjp.spring.data.query.fetch.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.query.fetch.service.ForumService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.*;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.LongStream;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAJoinFetchPaginationConfiguration.class)
public class SpringDataJPAOSIVTest extends AbstractSpringTest {

    public static final int POST_COUNT = 100;
    public static final int COMMENT_COUNT = 10;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ForumService forumService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CacheManager cacheManager;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

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
    }

    @Test
    public void testOSIV() {
        try(EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            EntityManagerHolder entityManagerHolder = new EntityManagerHolder(entityManager);
            TransactionSynchronizationManager.bindResource(
                entityManagerFactory,
                entityManagerHolder
            );

            Page<Post> posts = transactionTemplate.execute(
                status -> postRepository.findAll(
                    Example.of(
                        new Post()
                            .setTitle("High-Performance Java Persistence"),
                        ExampleMatcher.matching()
                            .withStringMatcher(ExampleMatcher.StringMatcher.STARTING)
                            .withIgnorePaths(Post_.CREATED_ON)
                    ),
                    PageRequest.of(0, 10, Sort.by(Post_.CREATED_ON))
                )
            );

            for(Post post : posts) {
                LOGGER.info("Post has {} comments", post.getComments().size());
            }
        } finally {
            TransactionSynchronizationManager.unbindResource(entityManagerFactory);
        }
    }
}


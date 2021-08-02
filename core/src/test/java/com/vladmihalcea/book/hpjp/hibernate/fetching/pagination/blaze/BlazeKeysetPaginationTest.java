package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.blaze;

import com.blazebit.persistence.*;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.BlazePersistenceCriteriaTest;
import com.vladmihalcea.book.hpjp.hibernate.fetching.PostCommentSummary;
import com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.DistinctPostResultTransformer;
import com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.Post;
import com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.PostComment;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BlazeKeysetPaginationTest extends AbstractTest {

    private CriteriaBuilderFactory cbf;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class,
            PostComment.class,
        };
    }

    @Override
    protected EntityManagerFactory newEntityManagerFactory() {
        EntityManagerFactory entityManagerFactory = super.newEntityManagerFactory();
        CriteriaBuilderConfiguration config = Criteria.getDefault();
        cbf = config.createCriteriaBuilderFactory(entityManagerFactory);
        return entityManagerFactory;
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    public static final int COMMENT_COUNT = 5;

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(
            AvailableSettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
            Boolean.FALSE.toString()
        );
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            LocalDateTime timestamp = LocalDateTime.of(
                2018, 10, 9, 12, 0, 0, 0
            );

            LongStream.rangeClosed(1, 50)
            .forEach(postId -> {
                Post post = new Post()
                .setId(postId)
                .setTitle(
                    String.format("High-Performance Java Persistence - Chapter %d",
                    postId)
                )
                .setCreatedOn(
                    Timestamp.valueOf(timestamp.plusMinutes(postId))
                );

                LongStream.rangeClosed(1, COMMENT_COUNT)
                .forEach(commentOffset -> {
                    long commentId = ((postId - 1) * COMMENT_COUNT) + commentOffset;

                    post.addComment(
                        new PostComment()
                        .setId(commentId)
                        .setReview(
                            String.format("Comment nr. %d - A must-read!", commentId)
                        )
                        .setCreatedOn(
                            Timestamp.valueOf(timestamp.plusMinutes(commentId))
                        )
                    );

                });

                entityManager.persist(post);
            });
        });
    }

    @Test
    public void testKeysetPagination() {
        doInJPA(entityManager -> {
            PagedList<Post> postPage = cbf
                .create(entityManager, Post.class)
                .orderByAsc("createdOn")
                .orderByAsc("id")
                .page(0, 10)
                .withKeysetExtraction(true)
                .getResultList();

            LOGGER.info("Post ids: {}", postPage.stream().map(Post::getId).collect(Collectors.toList()));
            LOGGER.info("Entry count: {}", postPage.getTotalSize());
            LOGGER.info("Page count: {}", postPage.getTotalPages());
            LOGGER.info("Current page number: {}", postPage.getPage());

            postPage = cbf
                .create(entityManager, Post.class)
                .orderByAsc("createdOn")
                .orderByAsc("id")
                .page(0, postPage.getMaxResults())
                .afterKeyset(postPage.getKeysetPage().getHighest())
                .withKeysetExtraction(true)
                .getResultList();

            LOGGER.info("Current page number: {}", postPage.getPage());
            LOGGER.info("Post ids: {}", postPage.stream().map(Post::getId).collect(Collectors.toList()));

            postPage = cbf
                .create(entityManager, Post.class)
                .orderByAsc("createdOn")
                .orderByAsc("id")
                .page(0, postPage.getMaxResults())
                .afterKeyset(postPage.getKeysetPage().getHighest())
                .withKeysetExtraction(true)
                .getResultList();

            LOGGER.info("Current page number: {}", postPage.getPage());
            LOGGER.info("Post ids: {}", postPage.stream().map(Post::getId).collect(Collectors.toList()));
        });
    }
}

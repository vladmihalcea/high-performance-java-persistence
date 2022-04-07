package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.blaze;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.Post;
import com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.PostComment;
import com.vladmihalcea.book.hpjp.hibernate.fetching.pagination.Post_;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.stream.LongStream;

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
                2021, 10, 9, 12, 0, 0, 0
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
            int pageSize = 10;

            PagedList<Post> postPage = cbf
                .create(entityManager, Post.class)
                .orderByAsc(Post_.CREATED_ON)
                .orderByAsc(Post_.ID)
                .page(0, pageSize)
                .withKeysetExtraction(true)
                .getResultList();

            LOGGER.info("Matching entity count: {}", postPage.getTotalSize());
            LOGGER.info("Page count: {}", postPage.getTotalPages());
            LOGGER.info("Current page number: {}", postPage.getPage());
            LOGGER.info("Post ids: {}",
                postPage.stream()
                    .map(Post::getId)
                    .toList()
            );

            postPage = cbf
                .create(entityManager, Post.class)
                .orderByAsc(Post_.CREATED_ON)
                .orderByAsc(Post_.ID)
                .page(
                    postPage.getKeysetPage(),
                    postPage.getPage() * postPage.getMaxResults(),
                    postPage.getMaxResults()
                )
                .getResultList();

            LOGGER.info("Current page number: {}", postPage.getPage());
            LOGGER.info("Post ids: {}",
                postPage.stream()
                    .map(Post::getId)
                    .toList()
            );

            postPage = cbf
                .create(entityManager, Post.class)
                .orderByAsc(Post_.CREATED_ON)
                .orderByAsc(Post_.ID)
                .page(
                    postPage.getKeysetPage(),
                    postPage.getPage() * postPage.getMaxResults(),
                    postPage.getMaxResults()
                )
                .withKeysetExtraction(true)
                .getResultList();

            LOGGER.info("Current page number: {}", postPage.getPage());
            LOGGER.info("Post ids: {}",
                postPage.stream()
                    .map(Post::getId)
                    .toList()
            );
        });
    }
}

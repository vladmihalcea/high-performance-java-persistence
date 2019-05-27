package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post_;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.junit.Test;

import javax.persistence.criteria.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class FailOnPaginationWithCollectionFetchTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class,
                PostComment.class,
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    public static final int COMMENT_COUNT = 5;

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty("hibernate.query.fail_on_pagination_over_collection_fetch", "true");
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            LocalDateTime timestamp = LocalDateTime.of(
                    2018, 10, 9, 12, 0, 0, 0
            );

            LongStream.range(1, 50).forEach(postId -> {
                Post post = new Post()
                        .setId(postId)
                        .setTitle(String.format("High-Performance Java Persistence - Chapter %d", postId))
                        .setCreatedOn(
                                Timestamp.valueOf(timestamp.plusMinutes(postId))
                        );

                LongStream.range(1, COMMENT_COUNT + 1).forEach(commentOffset -> {
                    long commentId = ((postId - 1) * COMMENT_COUNT) + commentOffset;

                    post.addComment(
                            new PostComment()
                                    .setId(commentId)
                                    .setReview(
                                            String.format("Comment nr. %d - A must read!", commentId)
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
    public void testFetchAndPaginate() {
        doInJPA(entityManager -> {
            try {
                entityManager.createQuery(
                    "select p " +
                    "from Post p " +
                    "left join fetch p.comments " +
                    "where p.title like :titlePattern " +
                    "order by p.createdOn", Post.class)
                .setParameter("titlePattern", "High-Performance Java Persistence %")
                .setMaxResults(5)
                .getResultList();

                fail("Should have thrown Exception");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("In memory pagination was about to be applied"));
            }
        });
    }

    @Test
    public void testFetchAndPaginateWithCriteriaApi() {
        doInJPA(entityManager -> {
            try {
                CriteriaBuilder builder = entityManager.getCriteriaBuilder();
                CriteriaQuery<Post> criteria = builder.createQuery(Post.class);

                Root<Post> post = criteria.from(Post.class);
                post.fetch("comments");

                ParameterExpression<String> parameterExpression = builder.parameter(String.class);

                criteria.where(
                    builder.like(
                        post.get("title"),
                        parameterExpression
                    )
                )
                .orderBy(
                    builder.asc(
                        post.get("createdOn")
                    )
                );

                entityManager
                .createQuery(criteria)
                .setParameter(parameterExpression, "High-Performance Java Persistence %")
                .setMaxResults(5)
                .getResultList();

                fail("Should have thrown Exception");
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("In memory pagination was about to be applied"));
            }
        });
    }
}

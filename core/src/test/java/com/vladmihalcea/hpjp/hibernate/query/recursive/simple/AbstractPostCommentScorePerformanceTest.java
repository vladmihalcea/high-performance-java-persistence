package com.vladmihalcea.hpjp.hibernate.query.recursive.simple;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.vladmihalcea.hpjp.hibernate.query.recursive.PostCommentScore;
import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
public abstract class AbstractPostCommentScorePerformanceTest extends AbstractPostgreSQLIntegrationTest {

    protected MetricRegistry metricRegistry = new MetricRegistry();

    protected com.codahale.metrics.Timer timer = metricRegistry.timer(getClass().getSimpleName());

    protected Slf4jReporter logReporter = Slf4jReporter
            .forRegistry(metricRegistry)
            .outputTo(LOGGER)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class,
        };
    }

    @Parameter(0)
    private int postCount;
    @Parameter(1)
    private int commentCount;

    public static Stream<Arguments> parameters() {
        int postCount = 10;
        return Stream.of(
            Arguments.of(postCount, 4),
            Arguments.of(postCount, 8),
            Arguments.of(postCount, 16),
            Arguments.of(postCount, 32),
            Arguments.of(postCount, 64)
        );
    }

    @Override
    public void afterInit() {
        for (long i = 0; i < postCount; i++) {
            insertPost(i);
        }
    }

    private int randomScore() {
        double random = Math.random() + 10;
        return (int) random;
    }

    private void insertPost(Long postId) {
        doInJPA(entityManager -> {
            Post post = new Post();
            post.setId(postId);
            post.setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            for (int i = 0; i < commentCount; i++) {
                PostComment comment1 = new PostComment();
                comment1.setPost(post);
                comment1.setReview(String.format("Comment %d", i));
                comment1.setScore(randomScore());
                entityManager.persist(comment1);

                for (int j = 0; j < commentCount / 2; j++) {
                    PostComment comment1_1 = new PostComment();
                    comment1_1.setParent(comment1);
                    comment1_1.setPost(post);
                    comment1_1.setReview(String.format("Comment %d-%d", i, j));
                    comment1_1.setScore(randomScore());
                    entityManager.persist(comment1_1);

                    for (int k = 0; k < commentCount / 4 ; k++) {
                        PostComment comment1_1_1 = new PostComment();
                        comment1_1_1.setParent(comment1_1_1);
                        comment1_1_1.setPost(post);
                        comment1_1_1.setReview(String.format("Comment %d-%d-%d", i, j, k));
                        comment1_1_1.setScore(randomScore());
                        entityManager.persist(comment1_1_1);
                    }
                    entityManager.flush();
                }
                entityManager.flush();
                entityManager.clear();
            }

            LOGGER.info("Added {} PostComments", entityManager
                    .createQuery("select count(pc) from PostComment pc where pc.post = :post")
                    .setParameter("post", post)
                    .getSingleResult()
            );
        });
    }

    @Override
    protected Properties properties() {
        Properties properties = super.properties();
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
        return properties;
    }

    @Test
    @Disabled
    public void test() {
        int rank = 3;
        int iterations = 25;
        for (int i = 0; i < iterations; i++) {
            for (long postId = 0; postId < postCount; postId++) {
                List<PostCommentScore> result = postCommentScores(postId, rank);
                assertNotNull(result);
            }
        }
        logReporter.report();
    }

    protected abstract List<PostCommentScore> postCommentScores(Long postId, int rank);
}

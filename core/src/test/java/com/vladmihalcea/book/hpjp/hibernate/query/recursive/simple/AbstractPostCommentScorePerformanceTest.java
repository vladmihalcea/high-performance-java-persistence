package com.vladmihalcea.book.hpjp.hibernate.query.recursive.simple;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.vladmihalcea.book.hpjp.hibernate.query.recursive.PostCommentScore;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
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

    private int postCount;
    private int commentCount;

    public AbstractPostCommentScorePerformanceTest(int postCount, int commentCount) {
        this.postCount = postCount;
        this.commentCount = commentCount;
    }

    @Parameterized.Parameters
    public static Collection<Integer[]> parameters() {
        List<Integer[]> postCountSizes = new ArrayList<>();
        int postCount = 2;
        /*postCountSizes.add(new Integer[] {postCount, 16});
        postCountSizes.add(new Integer[] {postCount, 4});
        postCountSizes.add(new Integer[] {postCount, 8});
        postCountSizes.add(new Integer[] {postCount, 16});
        postCountSizes.add(new Integer[] {postCount, 24});
        postCountSizes.add(new Integer[] {postCount, 32});
        postCountSizes.add(new Integer[] {postCount, 48});*/
        postCountSizes.add(new Integer[] {postCount, 64});
        return postCountSizes;
    }

    @Override
    public void init() {
        super.init();
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
        properties.put("hibernate.jdbc.batch_versioned_data", "true");
        return properties;
    }

    @Test
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

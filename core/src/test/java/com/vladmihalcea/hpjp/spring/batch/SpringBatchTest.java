package com.vladmihalcea.hpjp.spring.batch;

import com.vladmihalcea.hpjp.spring.batch.config.SpringBatchConfiguration;
import com.vladmihalcea.hpjp.spring.batch.domain.Post;
import com.vladmihalcea.hpjp.spring.batch.domain.PostStatus;
import com.vladmihalcea.hpjp.spring.batch.service.ForumService;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringBatchConfiguration.class)
public class SpringBatchTest extends AbstractSpringTest {

    public static final int POST_COUNT = 5 * 1000;

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Test
    public void testBatchWrite() {
        List<Post> posts = LongStream.rangeClosed(1, POST_COUNT)
            .mapToObj(postId -> new Post()
                .setId(postId)
                .setTitle(
                    String.format("High-Performance Java Persistence - Page %d",
                        postId
                    )
                )
                .setStatus(PostStatus.PENDING)
            )
            .toList();

        forumService.createPosts(posts);

        LongStream.rangeClosed(1, 1000).boxed().forEach(id -> assertNotNull(forumService.findById(id)));

        List<Post> matchedPosts = forumService.findByIds(
            LongStream.rangeClosed(1, 1000).boxed().toList()
        );
        assertEquals(1000, matchedPosts.size());
    }

    private int threadCount = 6;

    private long threadExecutionSeconds = TimeUnit.MINUTES.toSeconds(3);

    private ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    @Test
    @Ignore
    public void testRead() throws InterruptedException {
        int POST_COUNT = 1000;

        List<Post> posts = LongStream.rangeClosed(1, POST_COUNT)
            .mapToObj(postId -> new Post()
                .setId(postId)
                .setTitle(
                    String.format("High-Performance Java Persistence - Part %d",
                        postId
                    )
                )
                .setStatus(PostStatus.PENDING)
            )
            .toList();

        forumService.createPosts(posts);

        long startNanos = System.nanoTime();
        long endNanos = startNanos + TimeUnit.SECONDS.toNanos(threadExecutionSeconds);

        CountDownLatch awaitTermination = new CountDownLatch(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();

        ThreadLocalRandom random = ThreadLocalRandom.current();

        final AtomicBoolean failed = new AtomicBoolean();

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            tasks.add(
                () -> {
                    while (endNanos > System.nanoTime()) {
                        try {
                            Long id = random.nextLong(1, POST_COUNT);
                            LOGGER.info("Fetching entity by id [{}]", id);
                            Post post = forumService.findById(id);
                            assertNotNull(post);

                            sleep(250, TimeUnit.MILLISECONDS);
                        } catch (Exception e) {
                            LOGGER.warn(
                                String.format(
                                    "Thread [%d] execution failed", threadId
                                ),
                                e
                            );
                            failed.set(true);
                        }
                    }
                    awaitTermination.countDown();
                    return null;
                }
            );
        }

        executorService.invokeAll(tasks);
        awaitTermination.await();
        LOGGER.info("Finished processing");
    }

    private void sleep(long duration, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(duration));
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
}


package com.vladmihalcea.hpjp.spring.blaze;

import com.blazebit.persistence.PagedList;
import com.vladmihalcea.hpjp.spring.blaze.config.SpringBlazePersistenceConfiguration;
import com.vladmihalcea.hpjp.spring.blaze.domain.*;
import com.vladmihalcea.hpjp.spring.blaze.service.ForumService;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringBlazePersistenceConfiguration.class)
public class SpringBlazePersistenceKeysetPaginationTest extends AbstractSpringTest {

    public static final int POST_COUNT = 50;
    public static final int PAGE_SIZE = 25;

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            UserVote.class,
            PostComment.class,
            Post.class,
            Tag.class,
            User.class,
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                LocalDateTime timestamp = LocalDateTime.of(
                    2021, 12, 30, 12, 0, 0, 0
                );

                LongStream.rangeClosed(1, POST_COUNT).forEach(postId -> {
                    Post post = new Post()
                        .setId(postId)
                        .setTitle(
                            String.format("High-Performance Java Persistence - Chapter %d",
                                postId)
                        )
                        .setCreatedOn(
                            Timestamp.valueOf(timestamp.plusMinutes(postId))
                        );

                    entityManager.persist(post);
                });

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void test() {
        PagedList<Post> topPage = forumService.firstLatestPosts(PAGE_SIZE);

        assertEquals(POST_COUNT, topPage.getTotalSize());
        assertEquals(POST_COUNT / PAGE_SIZE, topPage.getTotalPages());
        assertEquals(1, topPage.getPage());
        List<Long> topIds = topPage.stream()
            .map(Post::getId)
            .toList();
        assertEquals(Long.valueOf(50), topIds.get(0));
        assertEquals(Long.valueOf(49), topIds.get(1));

        LOGGER.info("Top ids: {}", topIds);

        PagedList<Post> nextPage = forumService.findNextLatestPosts(topPage);

        assertEquals(2, nextPage.getPage());

        List<Long> nextIds = nextPage.stream()
            .map(Post::getId)
            .toList();
        assertEquals(Long.valueOf(25), nextIds.get(0));
        assertEquals(Long.valueOf(24), nextIds.get(1));

        LOGGER.info("Next ids: {}", nextIds);
    }
}


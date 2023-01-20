package com.vladmihalcea.book.hpjp.spring.batch;

import com.vladmihalcea.book.hpjp.spring.batch.config.SpringBatchYugabyteDBConfiguration;
import com.vladmihalcea.book.hpjp.spring.batch.domain.Post;
import com.vladmihalcea.book.hpjp.spring.batch.domain.PostStatus;
import com.vladmihalcea.book.hpjp.spring.batch.service.ForumService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.stream.LongStream;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringBatchYugabyteDBConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringBatchYugabyteDBTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final int POST_COUNT = 50 * 1000;

    @Autowired
    private ForumService forumService;

    @Test
    public void test() {
        List<Post> posts = LongStream.rangeClosed(1, POST_COUNT)
            .mapToObj(postId -> new Post()
                .setTitle(
                    String.format("High-Performance Java Persistence - Page %d",
                        postId
                    )
                )
                .setStatus(PostStatus.PENDING)
            )
            .toList();

        forumService.createPosts(posts);
    }
}


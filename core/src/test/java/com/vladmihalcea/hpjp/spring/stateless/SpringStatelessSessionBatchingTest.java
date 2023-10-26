package com.vladmihalcea.hpjp.spring.stateless;

import com.vladmihalcea.hpjp.spring.stateless.config.SpringStatelessSessionBatchingConfiguration;
import com.vladmihalcea.hpjp.spring.stateless.domain.Post;
import com.vladmihalcea.hpjp.spring.stateless.service.ForumService;
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
@ContextConfiguration(classes = SpringStatelessSessionBatchingConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringStatelessSessionBatchingTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final int POST_COUNT = 15;

    @Autowired
    private ForumService forumService;

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
                .setCreatedBy("Vlad Mihalcea")
                .setUpdatedBy("Vlad Mihalcea")
            )
            .toList();

        forumService.createPosts(posts);

        LongStream.rangeClosed(1, POST_COUNT)
            .boxed()
            .forEach(id -> {
                Post post = forumService.findById(id);
                if(post != null) {
                    LOGGER.info("Post [{}] found", post.getId());
                }
            });
    }
}


package com.vladmihalcea.hpjp.spring.stateless.sqlserver;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.stateless.sqlserver.config.SQLServerSpringStatelessSessionBatchingConfiguration;
import com.vladmihalcea.hpjp.spring.stateless.sqlserver.domain.Post;
import com.vladmihalcea.hpjp.spring.stateless.sqlserver.service.ForumService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.stream.LongStream;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SQLServerSpringStatelessSessionBatchingConfiguration.class)
public class SQLServerSpringStatelessSessionBatchingTest extends AbstractSpringTest {

    public static final int POST_COUNT = 15;

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


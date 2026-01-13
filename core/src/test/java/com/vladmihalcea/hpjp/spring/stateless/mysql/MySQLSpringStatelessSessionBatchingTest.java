package com.vladmihalcea.hpjp.spring.stateless;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.stateless.mysql.config.MySQLSpringStatelessSessionBatchingConfiguration;
import com.vladmihalcea.hpjp.spring.stateless.mysql.domain.Post;
import com.vladmihalcea.hpjp.spring.stateless.mysql.service.ForumService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.stream.LongStream;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = MySQLSpringStatelessSessionBatchingConfiguration.class)
public class MySQLSpringStatelessSessionBatchingTest extends AbstractSpringTest {

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


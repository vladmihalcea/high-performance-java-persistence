package com.vladmihalcea.hpjp.spring.partition;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.partition.config.SpringTablePartitioningConfiguration;
import com.vladmihalcea.hpjp.spring.partition.domain.Partition;
import com.vladmihalcea.hpjp.spring.partition.domain.Post;
import com.vladmihalcea.hpjp.spring.partition.domain.User;
import com.vladmihalcea.hpjp.spring.partition.repository.UserRepository;
import com.vladmihalcea.hpjp.spring.partition.service.ForumService;
import com.vladmihalcea.hpjp.spring.partition.util.UserContext;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.stream.LongStream;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringTablePartitioningConfiguration.class)
public class SpringTablePartitioningTest extends AbstractSpringTest {

    public static final int POST_COUNT = 3;

    @Autowired
    private ForumService forumService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class,
            User.class
        };
    }

    @Test
    public void test() {
        final User vlad = new User()
            .setFirstName("Vlad")
            .setLastName("Mihalcea")
            .setPartition(Partition.EUROPE);

        userRepository.persist(vlad);

        UserContext.logIn(vlad);

        forumService.createPosts(LongStream.rangeClosed(1, POST_COUNT)
            .mapToObj(postId -> new Post()
                .setTitle(
                    String.format("High-Performance Java Persistence - Part %d",
                        postId
                    )
                )
                .setUser(vlad)
            )
            .toList()
        );

        LongStream.rangeClosed(1, POST_COUNT).boxed()
            .forEach(id -> {
                Post post = forumService.findById(id);
                if (post != null) {
                    LOGGER.info("Post title: {}", post.getTitle());
                }
            });

        List<Post> posts = forumService.findByIds(
            LongStream.rangeClosed(1, POST_COUNT).boxed().toList()
        );
        assertTrue(posts.size() <= POST_COUNT);
    }
}


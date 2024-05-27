package com.vladmihalcea.hpjp.spring.data.base;

import com.vladmihalcea.hpjp.hibernate.forum.Post;
import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.base.config.SpringDataJPABaseRepositoryConfiguration;
import com.vladmihalcea.hpjp.spring.data.base.service.ForumService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPABaseRepositoryConfiguration.class)
public class SpringDataJPABaseRepositoryTest extends AbstractSpringTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class
        };
    }

    @Autowired
    private ForumService forumService;

    @Test
    public void test() {
        Long postId = forumService.createPost(
            new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
        ).getId();

        Post post = forumService.findById(postId);
        assertEquals("High-Performance Java Persistence", post.getTitle());

        post.setTitle("High-Performance Java Persistence, 2nd edition");
        forumService.updatePost(post);
    }
}


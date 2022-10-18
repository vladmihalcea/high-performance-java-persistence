package com.vladmihalcea.book.hpjp.spring.data.base;

import com.vladmihalcea.book.hpjp.hibernate.forum.Post;
import com.vladmihalcea.book.hpjp.spring.data.base.config.SpringDataJPABaseRepositoryConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.base.service.ForumService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPABaseRepositoryConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPABaseRepositoryTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

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


package com.vladmihalcea.hpjp.spring.data.jakarta;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.jakarta.config.SpringJakartaDataBasicConfiguration;
import com.vladmihalcea.hpjp.spring.data.jakarta.domain.Post;
import com.vladmihalcea.hpjp.spring.data.jakarta.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.jakarta.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.jakarta.service.ForumService;
import jakarta.inject.Inject;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringJakartaDataBasicConfiguration.class)
public class SpringJakartaDataBasicTest extends AbstractSpringTest {

    @Inject
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class,
            PostComment.class,
        };
    }

    @Test
    public void test() {
        Post post = forumService.addPost("High-Performance Java Persistence");
        PostComment comment = forumService.addPostComment("Awesome!", post.getId());

        List<PostComment> comments = forumService.findCommentsByReview(comment.getReview());
        assertEquals(1, comments.size());
        assertEquals(comment.getId(), comments.get(0).getId());

        comment.setReview("Highly recommended");
        forumService.updateComment(comment);

        comments = forumService.findCommentsByReview(comment.getReview());
        assertEquals(1, comments.size());
        assertEquals(comment.getId(), comments.get(0).getId());
    }
}


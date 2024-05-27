package com.vladmihalcea.hpjp.spring.data.merge;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.merge.config.SpringDataJPAMergeConfiguration;
import com.vladmihalcea.hpjp.spring.data.merge.domain.Post;
import com.vladmihalcea.hpjp.spring.data.merge.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.merge.service.ForumService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionCallback;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAMergeConfiguration.class)
public class SpringDataJPAMergeTest extends AbstractSpringTest {

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class,
        };
    }

    @Override
    public void afterInit() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(
                            String.format("High-Performance Java Persistence, Part no. %d", i)
                        )
                        .addComment(
                            new PostComment()
                                .setReview(
                                    String.format("Part no. %d review", i)
                                )
                        )
                );
            }
            return null;
        });
    }

    @Test
    public void testSaveAll() {
        List<Post> posts = forumService.findAllByTitleLike("High-Performance Java Persistence%");

        for (Post post : posts) {
            post.setTitle("Vlad Mihalcea's " + post.getTitle());
            for (PostComment comment : post.getComments()) {
                comment.setReview(comment.getReview() + " - ⭐⭐⭐⭐⭐");
            }
        }

        LOGGER.info("Save posts");
        forumService.saveAll(posts);
    }
}


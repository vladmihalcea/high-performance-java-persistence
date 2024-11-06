package com.vladmihalcea.hpjp.spring.data.query.multibag;

import com.vladmihalcea.hpjp.spring.common.AbstractSpringTest;
import com.vladmihalcea.hpjp.spring.data.query.multibag.config.SpringDataJPAMultipleBagFetchConfiguration;
import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.query.multibag.service.BrokenForumService;
import com.vladmihalcea.hpjp.spring.data.query.multibag.service.ForumService;
import org.hibernate.LazyInitializationException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@ContextConfiguration(classes = SpringDataJPAMultipleBagFetchConfiguration.class)
public class SpringDataJPAMultipleBagFetchTest extends AbstractSpringTest {

    public static final long POST_COUNT = 50;
    public static final long POST_COMMENT_COUNT = 20;
    public static final long TAG_COUNT = 10;

    @Autowired
    private BrokenForumService brokenForumService;

    @Autowired
    private ForumService forumService;

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            PostComment.class,
            Post.class,
            Tag.class
        };
    }

    @Override
    public void afterInit() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                List<Tag> tags = new ArrayList<>();

                for (long i = 1; i <= TAG_COUNT; i++) {
                    Tag tag = new Tag()
                        .setId(i)
                        .setName(String.format("Tag nr. %d", i));

                    entityManager.persist(tag);
                    tags.add(tag);
                }

                long commentId = 0;

                for (long postId = 1; postId <= POST_COUNT; postId++) {
                    Post post = new Post()
                        .setId(postId)
                        .setTitle(String.format("Post nr. %d", postId));


                    for (long i = 0; i < POST_COMMENT_COUNT; i++) {
                        post.addComment(
                            new PostComment()
                                .setId(++commentId)
                                .setReview("Excellent!")
                        );
                    }

                    for (int i = 0; i < TAG_COUNT; i++) {
                        post.getTags().add(tags.get(i));
                    }

                    entityManager.persist(post);
                }

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void testLazyInitializationException() {
        List<PostComment> comments = forumService.findAllCommentsByReview("Excellent!");

        try {
            for(PostComment comment : comments) {
                LOGGER.info("The post title is '{}'", comment.getPost().getTitle());
            }
        } catch (LazyInitializationException expected) {
            assertTrue(expected.getMessage().toLowerCase(Locale.ROOT).contains("could not initialize proxy"));
        }
    }
}


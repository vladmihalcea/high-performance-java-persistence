package com.vladmihalcea.hpjp.spring.data.query.multibag;

import com.vladmihalcea.hpjp.hibernate.fetching.LazyInitializationExceptionTest;
import com.vladmihalcea.hpjp.spring.data.query.multibag.config.SpringDataJPAMultipleBagFetchConfiguration;
import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Post;
import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.query.multibag.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.query.multibag.service.BrokenForumService;
import com.vladmihalcea.hpjp.spring.data.query.multibag.service.ForumService;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import jakarta.persistence.EntityManager;
import org.hibernate.LazyInitializationException;
import org.hibernate.loader.MultipleBagFetchException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAMultipleBagFetchConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAMultipleBagFetchTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final long POST_COUNT = 50;
    public static final long POST_COMMENT_COUNT = 20;
    public static final long TAG_COUNT = 10;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BrokenForumService brokenForumService;

    @Autowired
    private ForumService forumService;

    @Before
    public void init() {
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
            assertTrue(expected.getMessage().contains("could not initialize proxy"));
        }
    }
}


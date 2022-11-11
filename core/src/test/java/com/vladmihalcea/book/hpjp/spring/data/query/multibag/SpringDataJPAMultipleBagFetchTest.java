package com.vladmihalcea.book.hpjp.spring.data.query.multibag;

import com.vladmihalcea.book.hpjp.spring.data.query.multibag.config.SpringDataJPAMultipleBagFetchConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.query.multibag.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.query.multibag.domain.PostComment;
import com.vladmihalcea.book.hpjp.spring.data.query.multibag.domain.Tag;
import com.vladmihalcea.book.hpjp.spring.data.query.multibag.service.BrokenPostService;
import com.vladmihalcea.book.hpjp.spring.data.query.multibag.service.PostService;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
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

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAMultipleBagFetchConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAMultipleBagFetchTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final int POST_COUNT = 50;
    public static final int POST_COMMENT_COUNT = 20;
    public static final int TAG_COUNT = 10;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BrokenPostService brokenPostService;

    @Autowired
    private PostService postService;

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
    public void test() {
        try {
            List<Post> posts = brokenPostService.findAllWithCommentsAndTags(
                1L,
                POST_COUNT
            );

            fail("Should have thrown MultipleBagFetchException!");
        } catch (Exception expected) {
            MultipleBagFetchException rootCause = ExceptionUtil.rootCause(expected);
            LOGGER.error(
                "Cannot fetch the following collections simultaneously: {}",
                rootCause.getBagRoles()
            );
        }

        List<Post> posts = postService.findAllWithCommentsAndTags(
            1L,
            POST_COUNT
        );

        for (Post post : posts) {
            assertEquals(POST_COMMENT_COUNT, post.getComments().size());
            assertEquals(TAG_COUNT, post.getTags().size());
        }
    }
}


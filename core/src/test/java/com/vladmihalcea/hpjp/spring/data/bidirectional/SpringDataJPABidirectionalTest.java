package com.vladmihalcea.hpjp.spring.data.bidirectional;

import com.vladmihalcea.hpjp.spring.data.bidirectional.config.SpringDataJPABidirectionalConfiguration;
import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.Post;
import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.PostDetails;
import com.vladmihalcea.hpjp.spring.data.bidirectional.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.bidirectional.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.bidirectional.repository.PostRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPABidirectionalConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPABidirectionalTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Before
    public void init() {
        postRepository.persist(
            new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setDetails(new PostDetails().setCreatedBy("Vlad Mihalcea"))
                .addComment(
                    new PostComment()
                        .setReview("Best book on JPA and Hibernate!")
                )
                .addComment(
                    new PostComment()
                        .setReview("A must-read for every Java developer!")
                )
                .addTag(new Tag().setName("JDBC"))
                .addTag(new Tag().setName("Hibernate"))
                .addTag(new Tag().setName("jOOQ"))
        );
    }

    @Test
    public void testPersistPostCommentWithoutPostFetching() {
        LOGGER.info("Persisting PostComment without fetching the Post entity");
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            postCommentRepository.persist(
                new PostComment()
                    .setPost(postRepository.getReferenceById(1L))
                    .setReview("Very informative. Learned a lot, applied every day.")
            );

            return null;
        });
    }

    @Test
    public void testPersistPostCommentWithPostFetching() {
        LOGGER.info("Persisting PostComment when the Post entity was already fetched");
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = postRepository.findByIdWithDetailsAndComments(1L);

            post.addComment(
                new PostComment()
                    .setReview("Very informative. Learned a lot, applied every day.")
            );

            return null;
        });
    }
}


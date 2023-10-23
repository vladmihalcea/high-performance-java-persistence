package com.vladmihalcea.hpjp.spring.data.unidirectional;

import com.vladmihalcea.hpjp.spring.data.unidirectional.config.SpringDataJPAUnidirectionalConfiguration;
import com.vladmihalcea.hpjp.spring.data.unidirectional.domain.*;
import com.vladmihalcea.hpjp.spring.data.unidirectional.repository.*;
import com.vladmihalcea.hpjp.spring.data.unidirectional.service.ForumService;
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
@ContextConfiguration(classes = SpringDataJPAUnidirectionalConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAUnidirectionalTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostDetailsRepository postDetailsRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PostTagRepository postTagRepository;

    @Autowired
    private ForumService forumService;

    @Before
    public void init() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");
            postRepository.persist(post);

            postDetailsRepository.persist(
                new PostDetails()
                    .setCreatedBy("Vlad Mihalcea")
                    .setPost(post)
            );

            postCommentRepository.persist(
                new PostComment()
                    .setReview("Best book on JPA and Hibernate!")
                    .setPost(post)
            );

            postCommentRepository.persist(
                new PostComment()
                    .setReview("A must-read for every Java developer!")
                    .setPost(post)
            );

            Tag jdbc = new Tag().setName("JDBC");
            Tag hibernate = new Tag().setName("Hibernate");
            Tag jOOQ = new Tag().setName("jOOQ");

            tagRepository.persist(jdbc);
            tagRepository.persist(hibernate);
            tagRepository.persist(jOOQ);

            postTagRepository.persist(new PostTag(post, jdbc));
            postTagRepository.persist(new PostTag(post, hibernate));
            postTagRepository.persist(new PostTag(post, jOOQ));

            return null;
        });
    }

    @Test
    public void testDeleteWithBulk() {
        forumService.deletePostById(1L);
    }
}


package com.vladmihalcea.hpjp.spring.data.cascade;

import com.vladmihalcea.hpjp.spring.data.cascade.config.SpringDataJPACascadeConfiguration;
import com.vladmihalcea.hpjp.spring.data.cascade.domain.Post;
import com.vladmihalcea.hpjp.spring.data.cascade.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.cascade.domain.PostDetails;
import com.vladmihalcea.hpjp.spring.data.cascade.domain.Tag;
import com.vladmihalcea.hpjp.spring.data.cascade.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.cascade.repository.PostDetailsRepository;
import com.vladmihalcea.hpjp.spring.data.cascade.repository.PostRepository;
import com.vladmihalcea.hpjp.spring.data.query.example.domain.PostComment_;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPACascadeConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPACascadeTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostDetailsRepository postDetailsRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Test
    public void testSavePostAndComments() {
        postRepository.persist(
            new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .addComment(
                    new PostComment()
                        .setId(1L)
                        .setReview("Best book on JPA and Hibernate!")
                )
                .addComment(
                    new PostComment()
                        .setId(2L)
                        .setReview("A must-read for every Java developer!")
                )
        );

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = postRepository.findByIdWithComments(1L);
            postRepository.delete(post);

            return null;
        });
    }

    @Test
    public void testSavePostAndPostDetails() {
        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setDetails(
                    new PostDetails()
                        .setCreatedBy("Vlad Mihalcea")
                );

            postRepository.persist(post);
            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = postRepository.getReferenceById(1L);

            PostDetails postDetails = postDetailsRepository.findById(post.getId()).orElseThrow();
            assertEquals("Vlad Mihalcea", postDetails.getCreatedBy());

            return null;
        });

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            Post post = postRepository.findById(1L).orElseThrow();

            return null;
        });
    }
}


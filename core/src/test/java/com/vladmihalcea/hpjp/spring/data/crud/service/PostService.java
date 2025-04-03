package com.vladmihalcea.hpjp.spring.data.crud.service;

import com.vladmihalcea.hpjp.spring.data.crud.domain.Post;
import com.vladmihalcea.hpjp.spring.data.crud.domain.PostComment;
import com.vladmihalcea.hpjp.spring.data.crud.repository.PostCommentRepository;
import com.vladmihalcea.hpjp.spring.data.crud.repository.PostRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class PostService {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(r -> {
        Thread bob = new Thread(r);
        bob.setName("Bob");
        return bob;
    });

    @Transactional
    public PostComment addNewPostComment(String review, Long postId) {
        /*PostComment comment = new PostComment()
            .setReview(review)
            .setPost(postRepository.findById(postId).orElseThrow(
                ()-> new EntityNotFoundException(
                    String.format("Post with id [%d] was not found!", postId)
                )
            ));*/

        PostComment comment = new PostComment()
            .setReview(review)
            .setPost(postRepository.getReferenceById(postId));

        postCommentRepository.save(comment);

        return comment;
    }

    @Transactional
    public PostComment addNewPostCommentRaceCondition(String review, Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(
            ()-> new EntityNotFoundException(
                String.format("Post with id [%d] was not found!", postId)
            )
        );

        try {
            Integer updateCount = executorService.submit(() -> transactionTemplate.execute(status ->
                entityManager.createQuery("""
                    delete from Post
                    where id = :id
                    """)
                .setParameter("id", postId)
                .executeUpdate())
            ).get();

            assertEquals(1, updateCount.intValue());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        PostComment comment = new PostComment()
            .setReview(review)
            .setPost(post);

        postCommentRepository.save(comment);

        return comment;
    }
}

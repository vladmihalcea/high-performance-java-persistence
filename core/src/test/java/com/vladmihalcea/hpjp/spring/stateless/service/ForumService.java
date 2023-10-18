package com.vladmihalcea.hpjp.spring.stateless.service;

import com.vladmihalcea.hpjp.spring.stateless.domain.Post;
import com.vladmihalcea.hpjp.spring.stateless.repository.PostRepository;
import com.vladmihalcea.hpjp.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Vlad Mihalcea
 */
@Service
@Transactional(readOnly = true)
public class ForumService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForumService.class);

    private static final ExecutorService executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    private final PostRepository postRepository;

    private final TransactionTemplate transactionTemplate;

    private final int batchProcessingSize;

    public ForumService(
        @Autowired PostRepository postRepository,
        @Autowired TransactionTemplate transactionTemplate,
        @Autowired int batchProcessingSize) {
        this.postRepository = postRepository;
        this.transactionTemplate = transactionTemplate;
        this.batchProcessingSize = batchProcessingSize;
    }

    @Transactional(propagation = Propagation.NEVER)
    public void createPosts(List<Post> posts) {
        CollectionUtils.spitInBatches(posts, batchProcessingSize)
            .map(postBatch -> executorService.submit(() -> {
                try {
                    transactionTemplate.execute((status) -> postRepository.persistAll(postBatch));
                } catch (TransactionException e) {
                    LOGGER.error("Batch transaction failure", e);
                }
            }))
            .forEach(future -> {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    LOGGER.error("Batch execution failure", e);
                }
            });
    }

    public List<Post> findByIds(List<Long> ids) {
        return postRepository.findAllById(ids);
    }

    public Post findById(Long id) {
        return postRepository.findById(id).orElse(null);
    }
}

package com.vladmihalcea.book.hpjp.spring.data.query.exists;

import com.vladmihalcea.book.hpjp.spring.data.query.exists.config.SpringDataJPAExistsConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.query.exists.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.query.exists.domain.Post_;
import com.vladmihalcea.book.hpjp.spring.data.query.exists.repository.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.Assert.assertTrue;
import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.exact;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPAExistsConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPAExistsTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PostRepository postRepository;

    /*@Autowired
    private HypersistenceOptimizer hypersistenceOptimizer;*/

    @Before
    public void init() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                entityManager.persist(
                    new Post()
                        .setId(1L)
                        .setTitle("High-Performance Java Persistence")
                        .setSlug("high-performance-java-persistence")
                );


                entityManager.persist(
                    new Post()
                        .setId(2L)
                        .setTitle("Hypersistence Optimizer")
                        .setSlug("hypersistence-optimizer")
                );

                for (long i = 3; i <= 1000; i++) {
                    entityManager.persist(
                        new Post()
                            .setId(i)
                            .setTitle(String.format("Post %d", i))
                            .setSlug(String.format("post-%d", i))
                    );
                }

                return null;
            });
        } catch (TransactionException e) {
            LOGGER.error("Failure", e);
        }
    }

    @Test
    public void test() {
        String slug = "high-performance-java-persistence";

        //Query by id - Bad Idea
        assertTrue(
            postRepository.findBySlug(slug).isPresent()
        );

        //Query by example - Bad Idea
        assertTrue(
            postRepository.exists(
                Example.of(
                    new Post().setSlug(slug),
                    ExampleMatcher.matching()
                        .withIgnorePaths(Post_.ID)
                        .withMatcher(Post_.SLUG, exact())
                )
            )
        );

        //hypersistenceOptimizer.getEvents().clear();
        assertTrue(
            postRepository.existsById(1L)
        );
        //assertTrue(hypersistenceOptimizer.getEvents().isEmpty());
        //Query using exists - Okayish Idea
        assertTrue(
            postRepository.existsBySlug(slug)
        );
        //assertTrue(hypersistenceOptimizer.getEvents().isEmpty());

        //Query using exists - Okayish Idea
        assertTrue(
            postRepository.existsBySlugWithCount(slug)
        );

        assertTrue(
            postRepository.existsBySlugWithCase(slug)
        );
    }
}


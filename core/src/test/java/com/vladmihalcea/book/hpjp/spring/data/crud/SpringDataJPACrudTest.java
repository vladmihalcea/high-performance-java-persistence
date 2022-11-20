package com.vladmihalcea.book.hpjp.spring.data.crud;

import com.vladmihalcea.book.hpjp.hibernate.logging.validator.sql.SQLStatementCountValidator;
import com.vladmihalcea.book.hpjp.spring.data.crud.config.SpringDataJPACrudConfiguration;
import com.vladmihalcea.book.hpjp.spring.data.crud.domain.Post;
import com.vladmihalcea.book.hpjp.spring.data.crud.repository.PostRepository;
import com.vladmihalcea.book.hpjp.spring.data.crud.service.PostService;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDataJPACrudConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SpringDataJPACrudTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PostRepository postRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PostService postService;

    @Test
    public void testPersistAndMerge() {
        String slug = "high-performance-java-persistence";

        transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
            postRepository.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .setSlug("high-performance-java-persistence")
            );

            postRepository.persistAndFlush(
                new Post()
                    .setId(2L)
                    .setTitle("Hypersistence Optimizer")
                    .setSlug("hypersistence-optimizer")
            );

            postRepository.peristAllAndFlush(
                LongStream.range(3, 1000)
                    .mapToObj(i -> new Post()
                        .setId(i)
                        .setTitle(String.format("Post %d", i))
                        .setSlug(String.format("post-%d", i))
                    )
                    .collect(Collectors.toList())
            );

            return null;
        });

        List<Post> posts = transactionTemplate.execute(transactionStatus ->
            entityManager.createQuery("""
                select p
                from Post p
                where p.id < 10
                """, Post.class)
            .getResultList()
        );

        posts.forEach(post -> post.setTitle(post.getTitle() + " rocks!"));

        transactionTemplate.execute(transactionStatus ->
            postRepository.updateAll(posts)
        );
    }

    @Test
    public void testSave() {
        try {
            transactionTemplate.execute((TransactionCallback<Void>) transactionStatus -> {
                postRepository.save(
                    new Post()
                        .setId(1L)
                        .setTitle("High-Performance Java Persistence")
                        .setSlug("high-performance-java-persistence")
                );
                return null;
            });

            fail("Should throw UnsupportedOperationException!");
        } catch (UnsupportedOperationException expected) {
            LOGGER.warn("You shouldn't call the JpaRepository save method!");
        }
    }

    @Test
    @Ignore
    public void testSaveWithFindById() {
        Long postId = transactionTemplate.execute(transactionStatus -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setSlug("high-performance-java-persistence");

            postRepository.persist(post);

            return post.getId();
        });

        LOGGER.info("Save PostComment");

        postService.addNewPostComment("Best book on JPA and Hibernate!", postId);

        //The sequence call
        SQLStatementCountValidator.assertSelectCount(1);
        SQLStatementCountValidator.assertInsertCount(1);
    }

    @Test
    public void testSaveWithFindByIdRepeatableRead() {
        Long postId = transactionTemplate.execute(transactionStatus -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence")
                .setSlug("high-performance-java-persistence");

            postRepository.persist(post);

            return post.getId();
        });

        LOGGER.info("Save PostComment");

        try {
            postService.addNewPostCommentRaceCondition("Best book on JPA and Hibernate!", postId);
        } catch (DataIntegrityViolationException e) {
            LOGGER.error("Failure", e);
        }
    }
}


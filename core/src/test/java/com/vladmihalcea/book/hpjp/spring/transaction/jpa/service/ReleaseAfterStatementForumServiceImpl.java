package com.vladmihalcea.book.hpjp.spring.transaction.jpa.service;

import com.vladmihalcea.book.hpjp.hibernate.forum.dto.PostDTO;
import com.vladmihalcea.book.hpjp.hibernate.transaction.forum.Post;
import com.vladmihalcea.book.hpjp.spring.transaction.jpa.dao.PostDAO;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNotEquals;

/**
 * @author Vlad Mihalcea
 */
@Service
public class ReleaseAfterStatementForumServiceImpl implements ReleaseAfterStatementForumService {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final CountDownLatch latch1 = new CountDownLatch(1);

    private static final CountDownLatch latch2 = new CountDownLatch(1);

    private static final CountDownLatch latch3 = new CountDownLatch(1);

    @Autowired
    private PostDAO postDAO;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public Post newPost(String title) {
        Post post = new Post();
        post.setTitle(title);
        return postDAO.persist(post);
    }

    @Override
    @Transactional
    public PostDTO savePostTitle(Long id, String title) {
        Post post = postDAO.findById(id);

        post.setTitle(title);
        entityManager.flush();

        executorService.submit(() -> {
            transactionTemplate.execute(new TransactionCallback<Void>() {
                @Nullable
                @Override
                public Void doInTransaction(TransactionStatus status) {
                    awaitOnLatch(latch1);
                    try {
                        PostDTO _postDTO =  postDAO.getPostDTOById(id);
                        assertNotEquals(title, _postDTO.getTitle());
                    } catch (Throwable e) {
                        LOGGER.error("Failure", e);
                    }

                    entityManager.unwrap(Session.class).doWork(connection -> {
                        latch2.countDown();
                        awaitOnLatch(latch3);
                    });

                    return null;
                }
            });
        });

        latch1.countDown();
        awaitOnLatch(latch2);
        PostDTO postDTO =  postDAO.getPostDTOById(id);
        latch3.countDown();
        executorService.shutdownNow();

        return postDTO;
    }

    private static void awaitOnLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.interrupted();
        }
    }
}

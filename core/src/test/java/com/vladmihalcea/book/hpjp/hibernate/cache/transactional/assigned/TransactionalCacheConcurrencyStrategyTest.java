package com.vladmihalcea.book.hpjp.hibernate.cache.transactional.assigned;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.transaction.JPATransactionVoidFunction;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static com.vladmihalcea.book.hpjp.hibernate.cache.transactional.assigned.TransactionalEntities.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TransactionalCacheConcurrencyStrategyTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionalCacheConcurrencyStrategyTest extends AbstractTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Override
    protected void doInJPA(JPATransactionVoidFunction function) {
        transactionTemplate.execute((TransactionCallback<Void>) status -> {
            function.accept(entityManager);
            return null;
        });
    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[0];
    }

    @Override
    public EntityManagerFactory entityManagerFactory() {
        return entityManager.getEntityManagerFactory();
    }

    public void init() {
        doInJPA(entityManager -> {
            entityManager.createQuery("delete from PostComment").executeUpdate();
            entityManager.createQuery("delete from Post").executeUpdate();
            entityManager.getEntityManagerFactory().getCache().evictAll();
        });
        doInJPA(entityManager -> {
            entityManager.persist(
                new Post()
                    .setId(1L)
                    .setTitle("High-Performance Java Persistence")
                    .addComment(
                        new PostComment()
                            .setId(1L)
                            .setReview("JDBC part review")
                    )
                    .addComment(
                        new PostComment()
                            .setId(2L)
                            .setReview("Hibernate part review")
                    )
            );
        });
        doInJPA(entityManager -> {
            printEntityCacheRegionStatistics(Post.class);
            printEntityCacheRegionStatistics(PostComment.class);
            printCollectionCacheRegionStatistics(Post.class, "comments");

            LOGGER.info("Post entity inserted");
        });
    }

    @Override
    public void destroy() {

    }

    @Test
    public void testPostEntityLoad() {
        LOGGER.info("Load Post entity and comments collection");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            printEntityCacheRegionStatistics(Post.class);
            assertEquals(2, post.getComments().size());
            printCacheRegionStatistics(Post.class.getName() + ".comments");
        });
    }

    @Test
    public void testPostEntityEvictModifyLoad() {

        LOGGER.info("Evict, modify, load");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.detach(post);

            post.setTitle("High-Performance Hibernate");
            entityManager.merge(post);
            entityManager.flush();

            entityManager.detach(post);
            post = entityManager.find(Post.class, 1L);
            printEntityCacheRegionStatistics(Post.class);
        });
    }

    @Test
    public void testEntityUpdate() {
        LOGGER.debug("testEntityUpdate");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        doInJPA(entityManager -> {
            printCacheRegionStatistics(Post.class.getName());
            printCacheRegionStatistics(Post.class.getName() + ".comments");
            printCacheRegionStatistics(PostComment.class.getName());

            Post post = entityManager.find(Post.class, 1L);
            post.setTitle("High-Performance Hibernate");
            PostComment comment = post.getComments().remove(0);
            comment.setPost(null);

            entityManager.flush();

            printCacheRegionStatistics(Post.class.getName());
            printCacheRegionStatistics(Post.class.getName() + ".comments");
            printCacheRegionStatistics(PostComment.class.getName());

            LOGGER.debug("Commit after flush");
        });
        printCacheRegionStatistics(Post.class.getName());
        printCacheRegionStatistics(Post.class.getName() + ".comments");
        printCacheRegionStatistics(PostComment.class.getName());
    }

    @Test
    public void testEntityUpdateWithRollback() {
        LOGGER.debug("testEntityUpdate");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        try {
            doInJPA(entityManager -> {
                printCacheRegionStatistics(Post.class.getName());
                printCacheRegionStatistics(Post.class.getName() + ".comments");
                printCacheRegionStatistics(PostComment.class.getName());

                Post post = entityManager.find(Post.class, 1L);
                post.setTitle("High-Performance Hibernate");
                PostComment comment = post.getComments().remove(0);
                comment.setPost(null);

                entityManager.flush();

                printCacheRegionStatistics(Post.class.getName());
                printCacheRegionStatistics(Post.class.getName() + ".comments");
                printCacheRegionStatistics(PostComment.class.getName());

                if(comment.getId() != null) {
                    throw new IllegalStateException("Intentional roll back!");
                }
            });
        } catch (Exception expected) {
            LOGGER.info("Expected", expected);
        }
        printCacheRegionStatistics(Post.class.getName());
        printCacheRegionStatistics(Post.class.getName() + ".comments");
        printCacheRegionStatistics(PostComment.class.getName());
    }

    @Test
    public void testNonVersionedEntityUpdate() {
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
        });
        printCacheRegionStatistics(PostComment.class.getName());
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, 1L);
            comment.setReview("JDBC and Database part review");
        });
        printCacheRegionStatistics(PostComment.class.getName());
    }

    @Test
    public void testEntityDelete() {
        LOGGER.info("Cache entries can be deleted");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        printCacheRegionStatistics(Post.class.getName());
        printCacheRegionStatistics(Post.class.getName() + ".comments");
        printCacheRegionStatistics(PostComment.class.getName());

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            entityManager.remove(post);
        });

        printCacheRegionStatistics(Post.class.getName());
        printCacheRegionStatistics(Post.class.getName() + ".comments");
        printCacheRegionStatistics(PostComment.class.getName());

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertNull(post);
        });
    }
}

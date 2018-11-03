package com.vladmihalcea.book.hpjp.hibernate.cache.transactional.assigned;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vladmihalcea.book.hpjp.util.transaction.JPATransactionVoidFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Before
    public void init() {
        doInJPA(entityManager -> {
            entityManager.createQuery("delete from PostComment").executeUpdate();
            entityManager.createQuery("delete from Post").executeUpdate();
            entityManager.getEntityManagerFactory().getCache().evictAll();

            TransactionalEntities.Post post = new TransactionalEntities.Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");

            TransactionalEntities.PostComment comment1 = new TransactionalEntities.PostComment();
            comment1.setId(1L);
            comment1.setReview("JDBC part review");
            post.addComment(comment1);

            TransactionalEntities.PostComment comment2 = new TransactionalEntities.PostComment();
            comment2.setId(2L);
            comment2.setReview("Hibernate part review");
            post.addComment(comment2);

            entityManager.persist(post);
        });
        printCacheRegionStatistics(TransactionalEntities.Post.class.getName());
        printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
        LOGGER.info("Post entity inserted");
    }

    @Override
    public void destroy() {

    }

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[0];
    }

    @Override
    public EntityManagerFactory entityManagerFactory() {
        return entityManager.getEntityManagerFactory();
    }

    @Test
    public void testPostEntityLoad() {

        LOGGER.info("Load Post entity and comments collection");
        doInJPA(entityManager -> {
            TransactionalEntities.Post post = entityManager.find(TransactionalEntities.Post.class, 1L);
            assertEquals(2, post.getComments().size());
            printCacheRegionStatistics(post.getClass().getName());
            printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
        });
    }

    @Test
    public void testPostEntityEvictModifyLoad() {

        LOGGER.info("Evict, modify, load");

        doInJPA(entityManager -> {
            TransactionalEntities.Post post = entityManager.find(TransactionalEntities.Post.class, 1L);
            entityManager.detach(post);

            post.setTitle("High-Performance Hibernate");
            entityManager.merge(post);
            entityManager.flush();

            entityManager.detach(post);
            post = entityManager.find(TransactionalEntities.Post.class, 1L);
            printCacheRegionStatistics(post.getClass().getName());
        });
    }

    @Test
    public void testEntityUpdate() {
        LOGGER.debug("testEntityUpdate");

        doInJPA(entityManager -> {
            TransactionalEntities.Post post = entityManager.find(TransactionalEntities.Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        doInJPA(entityManager -> {
            printCacheRegionStatistics(TransactionalEntities.Post.class.getName());
            printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
            printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());

            TransactionalEntities.Post post = entityManager.find(TransactionalEntities.Post.class, 1L);
            post.setTitle("High-Performance Hibernate");
            TransactionalEntities.PostComment comment = post.getComments().remove(0);
            comment.setPost(null);

            entityManager.flush();

            printCacheRegionStatistics(TransactionalEntities.Post.class.getName());
            printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
            printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());

            LOGGER.debug("Commit after flush");
        });
        printCacheRegionStatistics(TransactionalEntities.Post.class.getName());
        printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
        printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());
    }

    @Test
    public void testEntityUpdateWithRollback() {
        LOGGER.debug("testEntityUpdate");

        doInJPA(entityManager -> {
            TransactionalEntities.Post post = entityManager.find(TransactionalEntities.Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        try {
            doInJPA(entityManager -> {
                printCacheRegionStatistics(TransactionalEntities.Post.class.getName());
                printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
                printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());

                TransactionalEntities.Post post = entityManager.find(TransactionalEntities.Post.class, 1L);
                post.setTitle("High-Performance Hibernate");
                TransactionalEntities.PostComment comment = post.getComments().remove(0);
                comment.setPost(null);

                entityManager.flush();

                printCacheRegionStatistics(TransactionalEntities.Post.class.getName());
                printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
                printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());

                if(comment.getId() != null) {
                    throw new IllegalStateException("Intentional roll back!");
                }
            });
        } catch (Exception expected) {
            LOGGER.info("Expected", expected);
        }
        printCacheRegionStatistics(TransactionalEntities.Post.class.getName());
        printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
        printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());
    }

    @Test
    public void testNonVersionedEntityUpdate() {
        doInJPA(entityManager -> {
            TransactionalEntities.PostComment comment = entityManager.find(TransactionalEntities.PostComment.class, 1L);
        });
        printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());
        doInJPA(entityManager -> {
            TransactionalEntities.PostComment comment = entityManager.find(TransactionalEntities.PostComment.class, 1L);
            comment.setReview("JDBC and Database part review");
        });
        printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());
    }

    @Test
    public void testEntityDelete() {
        LOGGER.info("Cache entries can be deleted");

        doInJPA(entityManager -> {
            TransactionalEntities.Post post = entityManager.find(TransactionalEntities.Post.class, 1L);
            assertEquals(2, post.getComments().size());
        });

        printCacheRegionStatistics(TransactionalEntities.Post.class.getName());
        printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
        printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());

        doInJPA(entityManager -> {
            TransactionalEntities.Post post = entityManager.find(TransactionalEntities.Post.class, 1L);
            entityManager.remove(post);
        });

        printCacheRegionStatistics(TransactionalEntities.Post.class.getName());
        printCacheRegionStatistics(TransactionalEntities.Post.class.getName() + ".comments");
        printCacheRegionStatistics(TransactionalEntities.PostComment.class.getName());

        doInJPA(entityManager -> {
            TransactionalEntities.Post post = entityManager.find(TransactionalEntities.Post.class, 1L);
            assertNull(post);
        });
    }
}

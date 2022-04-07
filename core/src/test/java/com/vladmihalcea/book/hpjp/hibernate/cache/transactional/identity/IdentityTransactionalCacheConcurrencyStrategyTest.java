package com.vladmihalcea.book.hpjp.hibernate.cache.transactional.identity;

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

import static com.vladmihalcea.book.hpjp.hibernate.cache.transactional.identity.IdentityTransactionalEntities.Post;
import static com.vladmihalcea.book.hpjp.hibernate.cache.transactional.identity.IdentityTransactionalEntities.PostComment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IdentityTransactionalCacheConcurrencyStrategyTestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class IdentityTransactionalCacheConcurrencyStrategyTest extends AbstractTest {

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

    private Post post;
    private  PostComment comment1;
    private  PostComment comment2;

    @Before
    public void init() {
        doInJPA(entityManager -> {
            entityManager.createQuery("delete from PostComment").executeUpdate();
            entityManager.createQuery("delete from Post").executeUpdate();
            entityManager.getEntityManagerFactory().getCache().evictAll();

            post = new Post();
            post.setTitle("High-Performance Java Persistence");

            comment1 = new PostComment();
            comment1.setReview("JDBC part review");
            post.addComment(comment1);

            comment2 = new PostComment();
            comment2.setReview("Hibernate part review");
            post.addComment(comment2);

            entityManager.persist(post);
        });
        printCacheRegionStatistics(Post.class.getName());
        printCollectionCacheRegionStatistics(Post.class, "comments");
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
            Post post = entityManager.find(Post.class, this.post.getId());
            assertEquals(2, post.getComments().size());
            printEntityCacheRegionStatistics(Post.class);
            printCollectionCacheRegionStatistics(Post.class, "comments");
        });
    }

    @Test
    public void testPostEntityEvictModifyLoad() {

        LOGGER.info("Evict, modify, load");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, this.post.getId());
            entityManager.detach(post);

            post.setTitle("High-Performance Hibernate");
            entityManager.merge(post);
            entityManager.flush();

            entityManager.detach(post);
            post = entityManager.find(Post.class, this.post.getId());
            printEntityCacheRegionStatistics(Post.class);
        });
    }

    @Test
    public void testEntityUpdate() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, this.post.getId());
            post.setTitle("High-Performance Hibernate");
            PostComment comment = post.getComments().remove(0);
            comment.setPost(null);

            entityManager.flush();

            printCacheRegionStatistics(Post.class.getName());
            printCollectionCacheRegionStatistics(Post.class, "comments");
            printCacheRegionStatistics(PostComment.class.getName());

            LOGGER.debug("Commit after flush");
        });
        printCacheRegionStatistics(Post.class.getName());
        printCollectionCacheRegionStatistics(Post.class, "comments");
        printCacheRegionStatistics(PostComment.class.getName());
    }

    @Test
    public void testNonVersionedEntityUpdate() {
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, this.comment1.getId());
        });
        printCacheRegionStatistics(PostComment.class.getName());
        doInJPA(entityManager -> {
            PostComment comment = entityManager.find(PostComment.class, this.comment1.getId());
            comment.setReview("JDBC and Database part review");
        });
        printCacheRegionStatistics(PostComment.class.getName());
    }

    @Test
    public void testEntityDelete() {
        LOGGER.info("Cache entries can be deleted");

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, this.post.getId());
            assertEquals(2, post.getComments().size());
        });

        printCacheRegionStatistics(Post.class.getName());
        printCollectionCacheRegionStatistics(Post.class, "comments");
        printCacheRegionStatistics(PostComment.class.getName());

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, this.post.getId());
            entityManager.remove(post);
        });

        printCacheRegionStatistics(Post.class.getName());
        printCollectionCacheRegionStatistics(Post.class, "comments");
        printCacheRegionStatistics(PostComment.class.getName());

        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, this.post.getId());
            assertNull(post);
        });
    }
}

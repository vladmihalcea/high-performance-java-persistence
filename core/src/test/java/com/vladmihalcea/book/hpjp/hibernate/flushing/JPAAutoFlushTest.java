package com.vladmihalcea.book.hpjp.hibernate.flushing;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Test;

import org.jboss.logging.Logger;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class JPAAutoFlushTest extends AbstractTest {

    private static final Logger log = Logger.getLogger(JPAAutoFlushTest.class);

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return false;
    }

    @Test
    public void testFlushAutoCommit() {
        EntityManager entityManager = null;
        EntityTransaction txn = null;
        try {
            entityManager = entityManagerFactory().createEntityManager();
            txn = entityManager.getTransaction();
            txn.begin();

            Post post = new Post("Hibernate");
            post.setId(1L);
            entityManager.persist(post);
            log.info("Entity is in persisted state");

            txn.commit();
        } catch (RuntimeException e) {
            if (txn != null && txn.isActive()) txn.rollback();
            throw e;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    @Test
    public void testFlushAutoJPQL() {
        doInJPA(entityManager -> {
            log.info("testFlushAutoJPQL");
            Post post = new Post("Hibernate");
            post.setId(1L);
            entityManager.persist(post);
            entityManager.createQuery("select p from Tag p").getResultList();
            entityManager.createQuery("select p from Post p").getResultList();
        });
    }

    @Test
    public void testFlushAutoJPQLOverlap() {
        doInJPA(entityManager -> {
            log.info("testFlushAutoJPQL");
            Post post = new Post("Hibernate");
            post.setId(1L);
            entityManager.persist(post);
            entityManager.createQuery("select p from PostDetails p").getResultList();
            entityManager.createQuery("select p from Post p").getResultList();
        });
    }

    @Test
    public void testFlushAutoSQL() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("delete from Post").executeUpdate();
        });
        doInJPA(entityManager -> {
            log.info("testFlushAutoSQL");

            assertTrue(((Number) entityManager
                .createNativeQuery("select count(*) from Post")
                .getSingleResult()).intValue() == 0);

            Post post = new Post("Hibernate");
            post.setId(1L);
            entityManager.persist(post);

            int count = ((Number) entityManager
                    .createNativeQuery("select count(*) from Post")
                    .getSingleResult()).intValue();

            assertTrue( count == 1 );
        });
    }
}

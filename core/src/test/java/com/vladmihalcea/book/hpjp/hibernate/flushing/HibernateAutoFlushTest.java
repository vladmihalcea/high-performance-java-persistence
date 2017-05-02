package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.jboss.logging.Logger;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class HibernateAutoFlushTest extends AbstractTest {

    private static final Logger log = Logger.getLogger(HibernateAutoFlushTest.class);

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Override
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return true;
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

            assertTrue( count == 0 );
        });
    }

    @Test
    public void testFlushAutoSQLNativeSessionWithoutSynchronization() {
        doInHibernate(session -> {
            log.info("testFlushAutoSQLNativeSession");

            assertTrue(((Number) session
                    .createQuery("select count(*) from Post")
                    .getSingleResult()).intValue() == 0);

            Post post = new Post("Hibernate");
            post.setId(1L);
            session.persist(post);

            int count = ((Number) session
                    .createNativeQuery("select count(*) from Post")
                    .uniqueResult()).intValue();

            assertTrue( count == 0 );
        });
    }

    @Test
    public void testFlushAutoSQLNativeSessionWithSynchronization() {
        doInHibernate(session -> {
            log.info("testFlushAutoSQLNativeSession");

            assertTrue(((Number) session
                    .createQuery("select count(*) from Post")
                    .getSingleResult()).intValue() == 0);

            Post post = new Post("Hibernate");
            post.setId(1L);
            session.persist(post);

            int count = ((Number) session
                    .createNativeQuery("select count(*) from Post")
                    .addSynchronizedEntityClass(Post.class)
                    .uniqueResult()).intValue();

            assertTrue( count == 1 );
        });
    }
}

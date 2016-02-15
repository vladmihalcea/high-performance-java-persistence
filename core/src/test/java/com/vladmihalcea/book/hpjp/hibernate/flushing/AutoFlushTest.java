package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.Post;
import static org.junit.Assert.assertTrue;

/**
 * <code>AutoFlushTest</code> - Auto Flush Test
 *
 * @author Vlad Mihalcea
 */
public class AutoFlushTest extends AbstractTest {

    private static final Logger log = Logger.getLogger(AutoFlushTest.class);

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
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
            assertTrue(((Number) entityManager
                .createNativeQuery("select count(*) from Post")
                .getSingleResult()).intValue() == 1);
        });
    }

    @Test
    public void testFlushAutoSQLNativeSession() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("delete from Post").executeUpdate();
            ;
        });
        doInJPA(entityManager -> {
            log.info("testFlushAutoSQLNativeSession");

            assertTrue(((Number) entityManager
                    .createQuery("select count(*) from Post")
                    .getSingleResult()).intValue() == 0);

            Post post = new Post("Hibernate");
            post.setId(1L);
            entityManager.persist(post);

            Session session = entityManager.unwrap(Session.class);
            assertTrue(((Number) session
                .createSQLQuery("select count(*) from Post")
                .uniqueResult()).intValue() == 0);
        });
    }
}

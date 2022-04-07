package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.jboss.logging.Logger;
import org.junit.Test;

import jakarta.persistence.FlushModeType;

import static com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider.Post;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class CommitFlushTest extends AbstractTest {

    private static final Logger log = Logger.getLogger(CommitFlushTest.class);

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testFlushJPQL() {
        doInJPA(entityManager -> {
            log.info("testFlushJPQL");
            Post post = new Post("Hibernate");
            post.setId(1L);
            entityManager.persist(post);
            entityManager.createQuery("select p from Tag p")
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
            entityManager.createQuery("select p from Post p")
                .setFlushMode(FlushModeType.COMMIT)
                .getResultList();
        });
    }

    @Test
    public void testFlushSQL() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("delete from Post")
                    .executeUpdate();
        });
        doInJPA(entityManager -> {
            log.info("testFlushSQL");
            Post post = new Post("Hibernate");
            post.setId(1L);
            entityManager.persist(post);
            assertTrue(((Number) entityManager
                .createNativeQuery("select count(*) from Post")
                .getSingleResult()).intValue() > 0);
        });
    }
}

package com.vladmihalcea.book.hpjp.hibernate.flushing;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.jboss.logging.Logger;
import org.junit.Test;

import static com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider.Post;
import static org.junit.Assert.assertTrue;

/**
 * <code>AlwaysFlushTest</code> - Always Flush Test
 *
 * @author Vlad Mihalcea
 */
public class AlwaysFlushTest extends AbstractPostgreSQLIntegrationTest {

    private static final Logger log = Logger.getLogger(AlwaysFlushTest.class);

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Test
    public void testFlushSQL() {
        doInJPA(entityManager -> {
            entityManager.createNativeQuery("delete from Post").executeUpdate();
        });
        doInJPA(entityManager -> {
            log.info("testFlushSQL");
            Post post = new Post("Hibernate");
            post.setId(1L);
            entityManager.persist(post);

            Session session = entityManager.unwrap(Session.class);
            assertTrue(((Number) session
                    .createSQLQuery("select count(*) from Post")
                    .setFlushMode(FlushMode.ALWAYS)
                    .uniqueResult()).intValue() > 0);
        });
    }
}

package com.vladmihalcea.book.hpjp.hibernate.query.timeout;

import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import org.hibernate.jpa.QueryHints;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerJPAQueryTimeoutTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.setProperty(
            QueryHints.SPEC_HINT_TIMEOUT, String.valueOf(1000)
        );
    }

    @Test
    public void testQueryTimeout() {
        doInJPA(entityManager -> {
            LOGGER.info("Start waiting");
            //Works only for queries executed via JPA and Hibernate
            try {
                entityManager.createNativeQuery("WAITFOR DELAY '00:00:02'").executeUpdate();

                fail("Should have thrown a query timeout!");
            } catch (Exception e) {
                LOGGER.info("Timeout triggered", e);
                assertTrue(ExceptionUtil.rootCause(e).getMessage().contains("The query has timed out"));
            }

            LOGGER.info("Done waiting");
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        @GeneratedValue
        private Integer id;

        private String title;

        public Integer getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }
    }
}

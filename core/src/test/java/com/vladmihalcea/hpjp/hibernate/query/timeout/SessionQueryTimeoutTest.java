package com.vladmihalcea.hpjp.hibernate.query.timeout;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.jpa.AvailableHints;
import org.junit.Test;
import org.postgresql.util.PSQLException;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SessionQueryTimeoutTest extends AbstractTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testJPATimeout() {
        doInJPA(entityManager -> {
            entityManager.persist(new Post().setTitle("High-Performance Java Persistence"));
            try {
                List<Tuple> posts = entityManager.createQuery("""
                    select p.id, pg_sleep(2)
                    from Post p
                    """, Tuple.class)
                .setHint(AvailableHints.HINT_TIMEOUT, String.valueOf(1))
                .getResultList();

                fail("Timeout failure expected");
            } catch (Exception e) {
                PSQLException rootCause = ExceptionUtil.rootCause(e);
                assertTrue(rootCause.getMessage().contains("canceling statement due to user request"));            }
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

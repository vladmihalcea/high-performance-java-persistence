package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.Oracle10gDialect;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class FollowOnLockingTest extends AbstractOracleXEIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            for (long i = 0; i < 5; i++) {
                Post post = new Post();
                post.setId(i);
                post.setTitle("High-Performance Java Persistence");
                post.setBody(String.format("Chapter %d summary", i));
                post.setStatus(PostStatus.PENDING);
                entityManager.persist(post);
            }
        });
    }

    @Test
    public void testPessimisticWrite() {
        LOGGER.info("Test lock contention");
        doInJPA(entityManager -> {
            List<Post> pendingPosts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.status = :status",
                Post.class)
            .setParameter("status", PostStatus.PENDING)
            .setMaxResults(5)
            //.setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .unwrap(org.hibernate.Query.class)
            .setLockOptions(new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(LockOptions.SKIP_LOCKED))
            .list();

            assertEquals(5, pendingPosts.size());
        });
    }

    @Test
    public void testUpgradeSkipLocked() {
        LOGGER.info("Test lock contention");
        doInJPA(entityManager -> {
            List<Post> pendingPosts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.status = :status",
                Post.class)
            .setParameter("status", PostStatus.PENDING)
            .setFirstResult(2)
            .unwrap(org.hibernate.Query.class)
            .setLockOptions(new LockOptions(LockMode.UPGRADE_SKIPLOCKED))
            .list();

            assertEquals(3, pendingPosts.size());
        });
    }

    @Test
    public void testUpgradeSkipLockedOrderBy() {
        LOGGER.info("Test lock contention");
        doInJPA(entityManager -> {
            List<Post> pendingPosts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.status = :status " +
                "order by p.id ",
                Post.class)
            .setParameter("status", PostStatus.PENDING)
            .setFirstResult(2)
            .unwrap(org.hibernate.Query.class)
            .setLockOptions(new LockOptions(LockMode.UPGRADE_SKIPLOCKED))
            .list();

            assertEquals(3, pendingPosts.size());
        });
    }

    @Test
    public void testUpgradeSkipLockedOrderByMaxResult() {
        LOGGER.info("Test lock contention");
        doInJPA(entityManager -> {
            List<Post> pendingPosts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.status = :status " +
                "order by p.id ",
                Post.class)
            .setParameter("status", PostStatus.PENDING)
            .setMaxResults(5)
            .unwrap(org.hibernate.Query.class)
            .setLockOptions(new LockOptions(LockMode.UPGRADE_SKIPLOCKED))
            .list();

            assertEquals(3, pendingPosts.size());
        });
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider() {
            @Override
            public String hibernateDialect() {
                return OracleDialect.class.getName();
            }
        };
    }

    public static class OracleDialect extends Oracle10gDialect {

        @Override
        public boolean useFollowOnLocking() {
            //return false;
            return super.useFollowOnLocking();
        }
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        private String body;

        @Enumerated
        private PostStatus status;

        @Version
        private int version;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public PostStatus getStatus() {
            return status;
        }

        public void setStatus(PostStatus status) {
            this.status = status;
        }
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM
    }
}

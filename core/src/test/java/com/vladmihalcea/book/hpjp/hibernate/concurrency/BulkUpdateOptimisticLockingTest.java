package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.transaction.VoidCallable;
import org.hibernate.annotations.DynamicUpdate;
import org.junit.Test;

import jakarta.persistence.*;
import jakarta.persistence.criteria.*;

import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BulkUpdateOptimisticLockingTest extends AbstractTest {

    private static final int SPAM_POST_COUNT = 10;

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

    public void afterInit() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= SPAM_POST_COUNT; i++) {
                entityManager.persist(
                    new Post()
                        .setId(i)
                        .setTitle(String.format("Spam post %d", i))
                );
            }
        });
    }

    @Test
    public void testLostUpdate() {
        doInJPA(entityManager -> {
            Post post = entityManager.find(Post.class, 1L);
            assertEquals(PostStatus.PENDING, post.getStatus());
            assertEquals(0, post.getVersion());

            executeSync(() -> doInJPA(
                _entityManager -> {
                    int updateCount = entityManager.createQuery("""
                        update Post
                        set status = :newStatus
                        where
                           status = :oldStatus and
                           lower(title) like :pattern
                        """)
                    .setParameter("oldStatus", PostStatus.PENDING)
                    .setParameter("newStatus", PostStatus.SPAM)
                    .setParameter("pattern", "%spam%")
                    .executeUpdate();

                    assertEquals(SPAM_POST_COUNT, updateCount);
                }
            ));

            post.setStatus(PostStatus.APPROVED);
        });
    }

    @Test
    public void testJPQL() {
        doInJPA(entityManager -> {
            int zeroVersionCount = entityManager.createQuery("""
                select count(p)
                from Post p
                where p.version = :version
			    """, Number.class)
            .setParameter("version", (short) 0)
            .getSingleResult()
            .intValue();

            assertEquals(SPAM_POST_COUNT, zeroVersionCount);

            int updateCount = entityManager.createQuery("""
                update Post
                set status = :newStatus
                where
                   status = :oldStatus and
                   lower(title) like :pattern
                """)
            .setParameter("oldStatus", PostStatus.PENDING)
            .setParameter("newStatus", PostStatus.SPAM)
            .setParameter("pattern", "%spam%")
            .executeUpdate();

            assertEquals(SPAM_POST_COUNT, updateCount);

            int oneVersionCount = entityManager.createQuery("""
                select count(p)
                from Post p
                where p.version = :version
			    """, Number.class)
            .setParameter("version", (short) 1)
            .getSingleResult()
            .intValue();

            assertEquals(0, oneVersionCount);
        });
    }

    @Test
    public void testJPQLWithVersion() {
        doInJPA(entityManager -> {
            int zeroVersionCount = entityManager.createQuery("""
                select count(p)
                from Post p
                where p.version = :version
                """, Number.class)
            .setParameter("version", (short) 0)
            .getSingleResult()
            .intValue();

            assertEquals(SPAM_POST_COUNT, zeroVersionCount);

            int updateCount = entityManager.createQuery("""
                update Post
                set
                   status = :newStatus,   
                   version = version + 1
                where
                   status = :oldStatus and
                   lower(title) like :pattern
                """)
            .setParameter("oldStatus", PostStatus.PENDING)
            .setParameter("newStatus", PostStatus.SPAM)
            .setParameter("pattern", "%spam%")
            .executeUpdate();

            assertEquals(SPAM_POST_COUNT, updateCount);

            int oneVersionCount = entityManager.createQuery("""
                select count(p)
                from Post p
                where p.version = :version
			    """, Number.class)
            .setParameter("version", (short) 1)
            .getSingleResult()
            .intValue();

            assertEquals(SPAM_POST_COUNT, oneVersionCount);
        });
    }

    @Test
    public void testHQL() {
        doInJPA(entityManager -> {
            int zeroVersionCount = entityManager.createQuery("""
                select count(p)
                from Post p
                where p.version = :version
			    """, Number.class)
            .setParameter("version", (short) 0)
            .getSingleResult()
            .intValue();

            assertEquals(SPAM_POST_COUNT, zeroVersionCount);

            int updateCount = entityManager.createQuery("""
                update versioned Post
                set status = :newStatus
                where
                   status = :oldStatus and
                   lower(title) like :pattern
                """)
            .setParameter("oldStatus", PostStatus.PENDING)
            .setParameter("newStatus", PostStatus.SPAM)
            .setParameter("pattern", "%spam%")
            .executeUpdate();

            assertEquals(SPAM_POST_COUNT, updateCount);

            int oneVersionCount = entityManager.createQuery("""
                select count(p)
                from Post p
                where p.version = :version
			    """, Number.class)
            .setParameter("version", (short) 1)
            .getSingleResult()
            .intValue();

            assertEquals(SPAM_POST_COUNT, oneVersionCount);
        });
    }

    @Test
    public void testCriteriaAPI() {
        doInJPA(entityManager -> {
            int zeroVersionCount = entityManager.createQuery("""
                select count(p)
                from Post p
                where p.version = :version
			    """, Number.class)
            .setParameter("version", (short) 0)
            .getSingleResult()
            .intValue();

            assertEquals(SPAM_POST_COUNT, zeroVersionCount);

            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaUpdate<Post> update = builder.createCriteriaUpdate(Post.class);

            Root<Post> root = update.from(Post.class);

            Expression<Boolean> wherePredicate = builder.and(
                builder.equal(root.get("status"), PostStatus.PENDING),
                builder.like(builder.lower(root.get("title")), "%spam%")
            );

            Path<Short> versionPath = root.get("version");
            Expression<Short> incrementVersion = builder.sum((short) 1, versionPath);

            update
                .set(root.get("status"), PostStatus.SPAM)
                .set(versionPath, incrementVersion)
                .where(wherePredicate);

            int updateCount = entityManager.createQuery(update).executeUpdate();

            assertEquals(SPAM_POST_COUNT, updateCount);

            int oneVersionCount = entityManager.createQuery("""
                select count(*)
                from Post p
                where p.version = :version
                """, Number.class)
            .setParameter("version", (short) 1)
            .getSingleResult()
            .intValue();

            assertEquals(SPAM_POST_COUNT, oneVersionCount);
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    @DynamicUpdate
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Enumerated(EnumType.ORDINAL)
        @Column(columnDefinition = "smallint")
        private PostStatus status = PostStatus.PENDING;

        @Version
        private short version;

        public Long getId() {
            return id;
        }

        public Post setId(Long id) {
            this.id = id;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Post setTitle(String title) {
            this.title = title;
            return this;
        }

        public PostStatus getStatus() {
            return status;
        }

        public void setStatus(PostStatus status) {
            this.status = status;
        }

        public short getVersion() {
            return version;
        }

        public Post setVersion(short version) {
            this.version = version;
            return this;
        }
    }

    public enum PostStatus {
        PENDING,
        APPROVED,
        SPAM
    }
}

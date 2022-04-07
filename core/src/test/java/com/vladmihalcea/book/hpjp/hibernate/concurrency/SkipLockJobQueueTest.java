package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.junit.Test;

import jakarta.persistence.*;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class SkipLockJobQueueTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[]{
                Post.class
        };
    }

    public void afterInit() {
        doInJPA(entityManager -> {
            for (long i = 1; i <= 10; i++) {
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
    public void testLockContention() {
        LOGGER.info("Test lock contention");

        final int postCount = 2;

        doInJPA(entityManager -> {
            assertEquals(
                    postCount,
                    getAndLockPosts(
                        entityManager,
                        PostStatus.PENDING,
                        postCount
                    ).size()
            );

            try {
                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        assertEquals(
                            postCount,
                            getAndLockPosts(
                                _entityManager,
                                PostStatus.PENDING,
                                postCount
                            ).size()
                        );
                    });
                });
            } catch (Exception e) {
                assertTrue(ExceptionUtil.rootCause(e).getMessage().contains("could not obtain lock on row in relation \"post\""));
            }
        });
    }

    public List<Post> getAndLockPosts(
                EntityManager entityManager,
                PostStatus status,
                int postCount) {
        return entityManager.createQuery("""
            select p
            from Post p
            where p.status = :status
            order by p.id
            """, Post.class)
        .setParameter("status", status)
        .setMaxResults(postCount)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .setHint(
            "jakarta.persistence.lock.timeout",
            LockOptions.NO_WAIT
        )
        .getResultList();
    }

    @Test
    public void testSkipLocked() {
        LOGGER.info("Test lock contention");

        final int postCount = 2;

        doInJPA(entityManager -> {
            LOGGER.debug("Alice wants to moderate {} Post(s)", postCount);
            List<Post> pendingPosts = getAndLockPostsWithSkipLocked(entityManager, PostStatus.PENDING, postCount);
            List<Long> ids = pendingPosts.stream().map(Post::getId).collect(toList());
            assertTrue(ids.size() == 2 && ids.contains(1L) && ids.contains(2L));

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    LOGGER.debug("Bob wants to moderate {} Post(s)", postCount);
                    List<Post> _pendingPosts = getAndLockPostsWithSkipLocked(_entityManager, PostStatus.PENDING, postCount);
                    List<Long> _ids = _pendingPosts.stream().map(Post::getId).collect(toList());
                    assertTrue(_ids.size() == 2 && _ids.contains(3L) && _ids.contains(4L));
                });
            });
        });
    }

    @Test
    public void testAliceLocksAll() {
        LOGGER.info("Test lock contention");
        doInJPA(entityManager -> {
            List<Post> pendingPosts = getAndLockPostsWithSkipLocked(entityManager, PostStatus.PENDING, 10);
            assertTrue(pendingPosts.size() == 10);

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    List<Post> _pendingPosts = getAndLockPostsWithSkipLocked(_entityManager, PostStatus.PENDING, 2);
                    assertTrue(_pendingPosts.size() == 0);
                });
            });
        });
    }

    @Test
    public void testSkipLockedMaxCountLessThanLockCount() {
        LOGGER.info("Test lock contention");
        doInJPA(entityManager -> {
            List<Post> pendingPosts = getAndLockPostsWithSkipLocked(entityManager, PostStatus.PENDING, 11);
            assertEquals(10, pendingPosts.size());
        });
    }

    public List<Post> getAndLockPostsWithSkipLocked(
                EntityManager entityManager,
                PostStatus status,
                int postCount) {
        return entityManager.createQuery("""
            select p
            from Post p
            where p.status = :status
            order by p.id
            """, Post.class)
        .setParameter("status", status)
        .setMaxResults(postCount)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .setHint("jakarta.persistence.lock.timeout", LockOptions.SKIP_LOCKED)
        .getResultList();
    }

    private List<Post> getAndLockPostsWithSkipLockedOracle(
            EntityManager entityManager,
            int lockCount,
            int maxResults,
            Integer maxCount) {
        LOGGER.debug("Attempting to lock {} Post(s) entities", maxResults);
        List<Post> posts= entityManager.createQuery("""
            select p
            from Post p
            where p.status = :status
            order by p.id
            """, Post.class)
        .setParameter("status", PostStatus.PENDING)
        .setMaxResults(maxResults)
        .unwrap(org.hibernate.query.Query.class)
        //Legacy hack - UPGRADE_SKIPLOCKED bypasses follow-on-locking
        //.setLockOptions(new LockOptions(LockMode.UPGRADE_SKIPLOCKED))
        .setLockOptions(new LockOptions(LockMode.PESSIMISTIC_WRITE)
            .setTimeOut(LockOptions.SKIP_LOCKED)
            //This is not really needed for this query but shows that you can control the follow-on locking mechanism
            .setFollowOnLocking(false)
        )
        .list();

        if(posts.isEmpty()) {
            if(maxCount == null) {
                maxCount = pendingPostCount(entityManager);
            }
            if(maxResults < maxCount || maxResults == lockCount) {
                maxResults += lockCount;
                return getAndLockPostsWithSkipLockedOracle(entityManager, lockCount, maxResults, maxCount);
            }
        }
        LOGGER.debug("{} Post(s) entities have been locked", posts.size());
        return posts;
    }

    private int pendingPostCount(EntityManager entityManager) {
        int postCount = ((Number) entityManager.createQuery(
            "select count(*) from Post where status = :status")
        .setParameter("status", PostStatus.PENDING)
        .getSingleResult()).intValue();

        LOGGER.debug("There are {} PENDING Post(s)", postCount);
        return postCount;
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

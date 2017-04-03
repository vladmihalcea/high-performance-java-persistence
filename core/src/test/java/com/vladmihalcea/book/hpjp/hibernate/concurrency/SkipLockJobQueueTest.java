package com.vladmihalcea.book.hpjp.hibernate.concurrency;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Collections;
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

    @Before
    public void init() {
        super.init();
        doInJPA(entityManager -> {
            for (long i = 0; i < 10; i++) {
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
        doInJPA(entityManager -> {
            List<Post> pendingPosts = entityManager.createQuery(
                "select p " +
                "from Post p " +
                "where p.status = :status",
                Post.class)
            .setParameter("status", PostStatus.PENDING)
            .setFirstResult(0)
            .setMaxResults(2)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint("javax.persistence.lock.timeout", 0)
            .getResultList();

            assertEquals(2, pendingPosts.size());

            try {
                executeSync(() -> {
                    doInJPA(_entityManager -> {
                        List<Post> _pendingPosts = _entityManager.createQuery(
                            "select p " +
                            "from Post p " +
                            "where p.status = :status", Post.class)
                        .setParameter("status", PostStatus.PENDING)
                        .setFirstResult(0)
                        .setMaxResults(2)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .setHint("javax.persistence.lock.timeout", 0)
                        .getResultList();
                    });
                });
            } catch (Exception e) {
                assertEquals(1, Arrays.asList(ExceptionUtils.getThrowables(e))
                    .stream()
                    .map(Throwable::getClass)
                    .filter(clazz -> clazz.equals(PessimisticLockException.class))
                    .count());
            }
        });
    }

    @Test
    public void testSkipLocked() {
        LOGGER.info("Test lock contention");
        doInJPA(entityManager -> {
            final int lockCount = 2;
            LOGGER.debug("Alice wants to moderate {} Post(s)", lockCount);
            List<Post> pendingPosts = pendingPosts(entityManager, lockCount);
            List<Long> ids = pendingPosts.stream().map(Post::getId).collect(toList());
            assertTrue(ids.size() == 2 && ids.contains(0L) && ids.contains(1L));

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    LOGGER.debug("Bob wants to moderate {} Post(s)", lockCount);
                    List<Post> _pendingPosts = pendingPosts(_entityManager, lockCount);
                    List<Long> _ids = _pendingPosts.stream().map(Post::getId).collect(toList());
                    assertTrue(_ids.size() == 2 && _ids.contains(2L) && _ids.contains(3L));
                });
            });
        });
    }

    @Test
    public void testAliceLocksAll() {
        LOGGER.info("Test lock contention");
        doInJPA(entityManager -> {
            List<Post> pendingPosts = pendingPosts(entityManager, 10);
            assertTrue(pendingPosts.size() == 10);

            executeSync(() -> {
                doInJPA(_entityManager -> {
                    List<Post> _pendingPosts = pendingPosts(_entityManager, 2);
                    assertTrue(_pendingPosts.size() == 0);
                });
            });
        });
    }

    @Test
    public void testSkipLockedMaxCountLessThanLockCount() {
        LOGGER.info("Test lock contention");
        doInJPA(entityManager -> {
            List<Post> pendingPosts = pendingPosts(entityManager, 11);
            assertEquals(10, pendingPosts.size());
        });
    }

    public List<Post> pendingPosts(EntityManager entityManager, int lockCount) {
        return pendingPosts(entityManager, lockCount, lockCount, null);
    }

    private List<Post> pendingPosts(EntityManager entityManager, int lockCount,
                                   int maxResults, Integer maxCount) {
        LOGGER.debug("Attempting to lock {} Post(s) entities", maxResults);
        List<Post> posts= entityManager.createQuery(
            "select p from Post p where p.status = :status", Post.class)
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
                return pendingPosts(entityManager, lockCount, maxResults, maxCount);
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

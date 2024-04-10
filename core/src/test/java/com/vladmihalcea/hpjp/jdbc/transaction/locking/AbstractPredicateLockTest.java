package com.vladmihalcea.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import jakarta.persistence.*;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractPredicateLockTest extends AbstractTest {

    public static final int WAIT_MILLIS = 500;

    protected final CountDownLatch aliceLatch = new CountDownLatch(1);
    protected final CountDownLatch bobLatch = new CountDownLatch(1);

    protected final AtomicLong POST_COMMENT_ID = new AtomicLong();

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("High-Performance Java Persistence");
            entityManager.persist(post);

            for (long i = 1; i <= 3; i++) {
                entityManager.persist(
                    new PostComment()
                        .setId(POST_COMMENT_ID.incrementAndGet())
                        .setReview(String.format("Comment nr. %d", i))
                        .setPost(post)
                );
            }
        });

    }

    @Test
    public void testRangeLockPreventsInsert() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.doWork(this::prepareConnection);
            List<PostComment> comments = session.createQuery(
                "select c " +
                "from PostComment c " +
                "where c.post.id = :id", PostComment.class)
            .setParameter("id", 1L)
            .setLockOptions(new LockOptions(LockMode.PESSIMISTIC_WRITE))
            .getResultList();

            executeAsync(() -> {
                try {
                    doInHibernate(_session -> {
                        _session.doWork(this::prepareConnection);

                        Post post = _session.getReference(Post.class, 1L);

                        PostComment comment = new PostComment();
                        comment.setId((long) comments.size() + 1);
                        comment.setReview(String.format("Comment nr. %d", comments.size() + 1));
                        comment.setPost(post);

                        _session.persist(comment);

                        aliceLatch.countDown();
                        _session.flush();
                        LOGGER.info("Insert {} prevented by explicit lock", prevented.get() ? "was" : "was not");
                        bobLatch.countDown();
                    });
                } catch (Exception e) {
                    if (ExceptionUtil.isLockTimeout(e)) {
                        prevented.set(true);
                        LOGGER.info("Insert {} prevented by explicit lock", prevented.get() ? "was" : "was not");
                        bobLatch.countDown();
                    }
                }
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    @Override
    protected boolean nativeHibernateSessionFactoryBootstrap() {
        return true;
    }

    @Test
    public void testRangeLockPreventsDelete() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);

            List<PostComment> comments = session.createQuery(
                "select c " +
                "from PostComment c " +
                "where c.post.id = :id", PostComment.class)
            .setParameter("id", 1L)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList();

            executeAsync(() -> {
                try {
                    doInHibernate(_session -> {
                        _session.unwrap(Session.class).doWork(this::prepareConnection);

                        aliceLatch.countDown();
                        _session.createNativeQuery(
                            "delete from post_comment where id = :id ")
                        .setParameter("id", 1L)
                        .executeUpdate();

                        LOGGER.info("Delete {} prevented by explicit lock", prevented.get() ? "was" : "was not");
                        bobLatch.countDown();
                    });
                } catch (Exception e) {
                    if (ExceptionUtil.isLockTimeout(e)) {
                        prevented.set(true);
                        LOGGER.info("Delete {} prevented by explicit lock", prevented.get() ? "was" : "was not");
                        bobLatch.countDown();
                    }
                }
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    @Test
    public void testRangeLockPreventsUpdate() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);

            List<PostComment> comments = session.createQuery(
                "select c " +
                "from PostComment c " +
                "where c.post.id = :id", PostComment.class)
            .setParameter("id", 1L)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList();

            executeAsync(() -> {
                try {
                    doInHibernate(_session -> {
                        _session.unwrap(Session.class).doWork(this::prepareConnection);

                        aliceLatch.countDown();
                        _session.createQuery(
                            "update PostComment " +
                            "set review = :review " +
                            "where id = :id")
                        .setParameter("review", "Great")
                        .setParameter("id", 1L)
                        .executeUpdate();

                        LOGGER.info("Update {} prevented by explicit lock", prevented.get() ? "was" : "was not");
                        bobLatch.countDown();
                    });
                } catch (Exception e) {
                    if (ExceptionUtil.isLockTimeout(e)) {
                        prevented.set(true);
                        LOGGER.info("Update {} prevented by explicit lock", prevented.get() ? "was" : "was not");
                        bobLatch.countDown();
                    }
                }
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    protected void prepareConnection(Connection connection) {
        setJdbcTimeout(connection);
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        public Long getId() {
            return id;
        }

        public PostComment setId(Long id) {
            this.id = id;
            return this;
        }

        public String getReview() {
            return review;
        }

        public PostComment setReview(String review) {
            this.review = review;
            return this;
        }

        public Post getPost() {
            return post;
        }

        public PostComment setPost(Post post) {
            this.post = post;
            return this;
        }
    }
}

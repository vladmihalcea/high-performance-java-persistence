package com.vladmihalcea.book.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.book.hpjp.jdbc.transaction.locking.AbstractPredicateLockTest;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.PostgreSQLDataSourceProvider;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.junit.Test;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLPredicateLockTest extends AbstractPredicateLockTest {

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new PostgreSQLDataSourceProvider();
    }

    @Test
    public void testParentForUpdatePreventsInsert() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);
            Post _post = session.createQuery(
                    "select p " +
                            "from Post p " +
                            "where p.id = :id", Post.class)
                    .setParameter("id", 1L)
                    .setLockOptions(new LockOptions(LockMode.PESSIMISTIC_WRITE))
                    .getSingleResult();

            executeAsync(() -> {
                doInHibernate(_session -> {
                    _session.unwrap(Session.class).doWork(this::prepareConnection);

                    Post post = _session.getReference(Post.class, 1L);

                    PostComment comment = new PostComment();
                    comment.setId((long) _post.getComments().size() + 1);
                    comment.setReview(String.format("Comment nr. %d", comment.getId()));
                    comment.setPost(post);

                    _session.persist(comment);

                    aliceLatch.countDown();
                    _session.flush();
                    LOGGER.info("Insert {} prevented by explicit parent lock", prevented.get() ? "was" : "was not");
                    bobLatch.countDown();
                });
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    @Test
    public void testParentForNoKeyUpdatePreventsInsert() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);
            Post _post = session.createNativeQuery(
                    "select * " +
                    "from Post " +
                    "where id = :id " +
                    "for no key update", Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            executeAsync(() -> {
                doInHibernate(_session -> {
                    _session.unwrap(Session.class).doWork(this::prepareConnection);

                    Post post = _session.getReference(Post.class, 1L);

                    PostComment comment = new PostComment();
                    comment.setId((long) _post.getComments().size() + 1);
                    comment.setReview(String.format("Comment nr. %d", comment.getId()));
                    comment.setPost(post);

                    _session.persist(comment);

                    aliceLatch.countDown();
                    _session.flush();
                    LOGGER.info("Insert {} prevented by explicit parent lock", prevented.get() ? "was" : "was not");
                    bobLatch.countDown();
                });
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    @Test
    public void testParentForNoKeyUpdatePreventsConcurrentUpdate() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);
            Number _postId = (Number) session.createNativeQuery(
                    "select id " +
                    "from Post " +
                    "where id = :id " +
                    "for no key update")
            .setParameter("id", 1L)
            .getSingleResult();

            executeAsync(() -> {
                doInHibernate(_session -> {
                    _session.unwrap(Session.class).doWork(this::prepareConnection);

                    Post post = _session.getReference(Post.class, 1L);

                    post.setTitle("High-Performance Hibernate");

                    aliceLatch.countDown();
                    _session.flush();
                    LOGGER.info("Update on non-conflicting column {} prevented by explicit parent lock", prevented.get() ? "was" : "was not");
                    bobLatch.countDown();
                });
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    @Test
    public void testParentForKeyShareNonConflictingConcurrentUpdate() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);
            Number _postId = (Number) session.createNativeQuery(
                    "select id " +
                    "from Post " +
                    "where id = :id " +
                    "for key share")
            .setParameter("id", 1L)
            .getSingleResult();

            executeAsync(() -> {
                doInHibernate(_session -> {
                    _session.unwrap(Session.class).doWork(this::prepareConnection);

                    Post post = _session.getReference(Post.class, 1L);

                    post.setTitle("High-Performance Hibernate");

                    aliceLatch.countDown();
                    _session.flush();
                    LOGGER.info("Update on non-conflicting column {} prevented by explicit parent lock", prevented.get() ? "was" : "was not");
                    bobLatch.countDown();
                });
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    @Test
    public void testParentForKeyShareConflictingNoFkConcurrentUpdate() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);
            String _postTitle = (String) session.createNativeQuery(
                    "select title " +
                    "from post " +
                    "where id = :id " +
                    "for key share")
            .setParameter("id", 1L)
            .getSingleResult();

            executeAsync(() -> {
                doInHibernate(_session -> {
                    _session.unwrap(Session.class).doWork(this::prepareConnection);

                    Post post = _session.getReference(Post.class, 1L);

                    post.setTitle("High-Performance Hibernate");

                    aliceLatch.countDown();
                    _session.flush();
                    LOGGER.info("Update on conflicting column {} prevented by explicit parent lock", prevented.get() ? "was" : "was not");
                    bobLatch.countDown();
                });
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    @Test
    public void testParentForKeyShareConflictingFkConcurrentUpdate() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);
            Number _postCommentId = (Number) session.createNativeQuery(
                    "select post_id " +
                    "from post_comment " +
                    "where id = :id " +
                    "for update")
            .setParameter("id", 1L)
            .getSingleResult();

            executeAsync(() -> {
                doInHibernate(_session -> {
                    _session.unwrap(Session.class).doWork(this::prepareConnection);

                    PostComment postComment = _session.getReference(PostComment.class, _postCommentId.longValue());

                    postComment.setPost(null);

                    aliceLatch.countDown();
                    _session.flush();
                    LOGGER.info("Update on conflicting FK column {} prevented by explicit parent lock", prevented.get() ? "was" : "was not");
                    bobLatch.countDown();
                });
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    @Test
    public void testParentForUpdateConflictingConcurrentDelete() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);
            Number _postCommentId = (Number) session.createNativeQuery(
                "select post_id " +
                "from post_comment " +
                "where id = :id " +
                "for update")
            .setParameter("id", 1L)
            .getSingleResult();

            executeAsync(() -> {
                try {
                    doInHibernate(_session -> {
                        _session.unwrap(Session.class).doWork(this::prepareConnection);

                        int updateCount = _session.createNativeQuery(
                            "delete from post_comment " +
                            "where id = :id ")
                        .setParameter("id", _postCommentId)
                        .executeUpdate();

                        assertEquals(1, updateCount);
                    });
                } catch (Exception expected) {
                    prevented.set(true);

                    aliceLatch.countDown();
                    LOGGER.info("Update on conflicting FK column {} prevented by explicit parent lock", prevented.get() ? "was" : "was not");
                    bobLatch.countDown();
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
    public void testParentReadLockPreventsInsert() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);
            Post _post = session.createQuery(
                "select p " +
                "from Post p " +
                "where p.id = :id", Post.class)
            .setParameter("id", 1L)
            .setLockOptions(new LockOptions(LockMode.PESSIMISTIC_READ))
            .getSingleResult();

            executeAsync(() -> {
                doInHibernate(_session -> {
                    _session.unwrap(Session.class).doWork(this::prepareConnection);

                    Post post = _session.getReference(Post.class, 1L);

                    PostComment comment = new PostComment();
                    comment.setId((long) _post.getComments().size() + 1);
                    comment.setReview(String.format("Comment nr. %d", comment.getId()));
                    comment.setPost(post);

                    _session.persist(comment);

                    aliceLatch.countDown();
                    _session.flush();
                    LOGGER.info("Insert {} prevented by explicit parent lock", prevented.get() ? "was" : "was not");
                    bobLatch.countDown();
                });
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }
}

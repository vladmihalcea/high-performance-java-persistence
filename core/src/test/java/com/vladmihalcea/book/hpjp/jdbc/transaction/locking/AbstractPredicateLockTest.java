package com.vladmihalcea.book.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.junit.Test;

import javax.persistence.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Vlad Mihalcea
 */
public abstract class AbstractPredicateLockTest extends AbstractTest {

    public static final int WAIT_MILLIS = 500;

    private final CountDownLatch aliceLatch = new CountDownLatch(1);
    private final CountDownLatch bobLatch = new CountDownLatch(1);

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    public void init() {
        super.init();
        doInHibernate(session -> {
            Post post = new Post();
            post.setId(1L);
            post.setTitle("High-Performance Java Persistence");
            session.persist(post);

            for (long i = 1; i <= 3; i++) {
                PostComment comment = new PostComment();
                comment.setId(i);
                comment.setReview(String.format("Comment nr. %d", i));
                post.addComment(comment);
            }
        });

    }

    @Test
    public void testRangeLockPreventsInsert() throws SQLException {
        AtomicBoolean prevented = new AtomicBoolean();

        doInHibernate( session -> {
            session.unwrap(Session.class).doWork(this::prepareConnection);
            List<PostComment> comments = session.createQuery(
                "select c " +
                "from PostComment c " +
                "where c.post.id = :id", PostComment.class)
            .setParameter("id", 1L)
            .setLockOptions(new LockOptions(LockMode.PESSIMISTIC_WRITE))
            .getResultList();

            executeAsync(() -> {
                doInHibernate(_session -> {
                    _session.unwrap(Session.class).doWork(this::prepareConnection);

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
            });

            awaitOnLatch(aliceLatch);
            sleep(WAIT_MILLIS);
            LOGGER.info("Alice woke up!");
            prevented.set(true);
        } );
        awaitOnLatch(bobLatch);
    }

    protected void prepareConnection(Connection connection) {

    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @OneToMany(cascade = CascadeType.ALL, mappedBy = "post",
                orphanRemoval = true)
        private List<PostComment> comments = new ArrayList<>();

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

        public List<PostComment> getComments() {
            return comments;
        }

        public void addComment(PostComment comment) {
            comments.add(comment);
            comment.setPost(this);
        }
    }

    @Entity(name = "PostComment")
    @Table(name = "post_comment")
    public static class PostComment {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Post post;

        private String review;

        public PostComment() {}

        public PostComment(String review) {
            this.review = review;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }
    }
}

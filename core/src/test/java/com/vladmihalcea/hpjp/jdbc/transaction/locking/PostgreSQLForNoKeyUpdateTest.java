package com.vladmihalcea.hpjp.jdbc.transaction.locking;

import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.hpjp.util.providers.Database;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLForNoKeyUpdateTest extends AbstractTest {

    protected final AtomicLong POST_COMMENT_ID = new AtomicLong();

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            Post post = new Post()
                .setId(1L)
                .setTitle("Transactions");

            entityManager.persist(post);
        });
    }

    @Test
    public void testParentForUpdatePreventsChildInsert(){
        AtomicBoolean prevented = new AtomicBoolean();

        doInJPA(entityManager -> {
            final Post _post = (Post) entityManager.createNativeQuery("""
                SELECT id, title
                FROM post p
                WHERE id = :id
                FOR UPDATE
                """, Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            executeSync(() -> {
                try {
                    doInStatelessSession(session -> {
                        session.doWork(this::setJdbcTimeout);

                        session.insert(
                            new PostComment()
                                .setId(POST_COMMENT_ID.incrementAndGet())
                                .setReview(String.format("Comment nr. %d", POST_COMMENT_ID.get()))
                                .setPost(_post)
                        );
                    });
                } catch (Exception e) {
                    prevented.set(ExceptionUtil.isLockTimeout(e));
                }
            });
        });

        assertTrue(prevented.get());
        LOGGER.info("Insert was prevented by the explicit parent lock");
    }

    @Test
    public void testParentForNoKeyUpdateAllowsChildInsert() {
        AtomicBoolean prevented = new AtomicBoolean();

        doInJPA(entityManager -> {
            final Post _post = (Post) entityManager.createNativeQuery("""
                SELECT id, title
                FROM post p
                WHERE id = :id
                FOR NO KEY UPDATE
                """, Post.class)
            .setParameter("id", 1L)
            .getSingleResult();

            executeSync(() -> {
                try {
                    doInStatelessSession(session -> {
                        session.doWork(this::setJdbcTimeout);

                        session.insert(
                            new PostComment()
                                .setId(POST_COMMENT_ID.incrementAndGet())
                                .setReview(String.format("Comment nr. %d", POST_COMMENT_ID.get()))
                                .setPost(_post)
                        );
                    });
                } catch (Exception e) {
                    prevented.set(ExceptionUtil.isLockTimeout(e));
                }
            });
        });

        assertFalse(prevented.get());
        LOGGER.info("Insert was not prevented by the explicit parent lock");
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        @Column(length = 100)
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

        @Column(length = 250)
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

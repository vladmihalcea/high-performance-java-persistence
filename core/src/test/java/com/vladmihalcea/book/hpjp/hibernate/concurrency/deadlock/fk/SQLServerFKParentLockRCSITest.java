package com.vladmihalcea.book.hpjp.hibernate.concurrency.deadlock.fk;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerFKParentLockRCSITest extends AbstractTest {

    private final int ISOLATION_LEVEL = Connection.TRANSACTION_READ_COMMITTED;

    public static final String LOCK_TABLE_TEMPLATE = """
        | table_name | blocking_session_id | wait_type | resource_type | request_status | request_mode | request_session_id |
        |------------|---------------------|-----------|---------------|----------------|--------------|--------------------|
        |%1$12s|%2$21s|%3$11s|%4$15s|%5$16s|%6$14s|%7$20s|
        """;

    @Override
    protected Class<?>[] entities() {
        return new Class<?>[] {
            Post.class,
            PostComment.class
        };
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            long postId = 1;
            long commentId = 1;
            for (long i = 0; i < 1000; i++) {
                Post post = new Post();
                post.setId(postId++);
                post.setTitle("High-Performance Java Persistence");
                entityManager.persist(post);

                for (long j = 0; j < 10; j++) {
                    PostComment comment = new PostComment();
                    comment.setId(commentId++);
                    comment.setReview("Awesome!");
                    comment.setPost(post);

                    entityManager.persist(comment);
                }

                if(i > 0 && i % 100 == 0) {
                    entityManager.flush();
                }
            }
        });
        executeStatement("ALTER DATABASE [high_performance_java_persistence] SET READ_COMMITTED_SNAPSHOT ON");
    }

    @Override
    public void destroy() {
        executeStatement("ALTER DATABASE [high_performance_java_persistence] SET READ_COMMITTED_SNAPSHOT OFF");
        super.destroy();
    }

    protected final ExecutorService monitoringExecutorService = Executors.newSingleThreadExecutor(r -> {
        Thread bob = new Thread(r);
        bob.setName("Monitoring");
        return bob;
    });

    /*
     */
    @Test
    public void test() {
       CountDownLatch bobStart = new CountDownLatch(1);
       CountDownLatch monitoringStart = new CountDownLatch(1);
        try {
            doInJPA(entityManager -> {
                LOGGER.info(
                    "Alice session id: {}",
                    entityManager.createNativeQuery("SELECT @@SPID").getSingleResult()
                );
                LOGGER.info("Alice updates the Post entity");
                Post post = entityManager.find(Post.class, 1L);
                post.setTitle("High-Performance Java Persistence 2nd edition");
                entityManager.flush();

                Future<?> bobFuture = executeAsync(() -> {
                    doInJPA(_entityManager -> {
                        prepareConnection(_entityManager);
                        LOGGER.info(
                            "Bob session id: {}",
                            _entityManager.createNativeQuery("SELECT @@SPID").getSingleResult()
                        );
                        LOGGER.info("Bob updates the PostComment entity");
                        PostComment _comment = _entityManager.find(PostComment.class, 1L);
                        _comment.setReview("Great!");
                        bobStart.countDown();
                        try {
                            _entityManager.flush();
                        } catch (Exception e) {
                            Exception rootException = ExceptionUtil.rootCause(e);
                            if(ExceptionUtil.isLockTimeout(rootException)) {
                                LOGGER.info("Lock timeout detected", rootException);
                            }
                        }
                    });
                });

                Future<?> monitoringFuture = monitoringExecutorService.submit(() -> {
                    doInJPA(_entityManager -> {
                        awaitOnLatch(monitoringStart);

                        List<Tuple> lockInfo = _entityManager.createNativeQuery("""
                            SELECT
                                table_name = schema_name(o.schema_id) + '.' + o.name,
                                wt.blocking_session_id,
                                wt.wait_type,
                                tm.resource_type,
                                tm.request_status,
                                tm.request_mode,
                                tm.request_session_id
                            FROM sys.dm_tran_locks AS tm
                            INNER JOIN sys.dm_os_waiting_tasks as wt ON tm.lock_owner_address = wt.resource_address
                            LEFT OUTER JOIN sys.partitions AS p ON p.hobt_id = tm.resource_associated_entity_id
                            LEFT OUTER JOIN sys.objects o ON o.object_id = p.object_id OR tm.resource_associated_entity_id = o.object_id
                            WHERE resource_database_id = DB_ID()
                            """, Tuple.class)
                        .getResultList();

                        if (!lockInfo.isEmpty()) {
                            Tuple result = lockInfo.get(0);

                            int i = 0;
                            LOGGER.info(
                                "Lock waiting info: \n{}",
                                String.format(
                                    LOCK_TABLE_TEMPLATE,
                                    result.get(i++),
                                    result.get(i++),
                                    result.get(i++),
                                    result.get(i++),
                                    result.get(i++),
                                    result.get(i++),
                                    result.get(i)
                                )
                            );
                        }
                    });
                });

                awaitOnLatch(bobStart);

                try {
                    monitoringStart.countDown();
                    bobFuture.get();
                    monitoringFuture.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (RuntimeException e) {
            Exception rootException = ExceptionUtil.rootCause(e);
            if(!ExceptionUtil.isLockTimeout(rootException)) {
                fail("Expected a lock timeout exception");
            }
        } finally {
            monitoringExecutorService.shutdownNow();
            executorService.shutdownNow();
        }
    }

    protected void prepareConnection(EntityManager entityManager) {
        entityManager.unwrap(Session.class).doWork(connection -> {
            connection.setTransactionIsolation(ISOLATION_LEVEL);
            setJdbcTimeout(connection);
        });
    }

    @Entity(name = "Post")
    @Table(name = "Post")
    public static class Post {

        @Id
        @Column(name = "PostID")
        private Long id;

        @Column(name = "Title")
        private String title;

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
    }

    @Entity(name = "PostComment")
    @Table(
        name = "PostComment",
        indexes = @Index(
            name = "FK_PostComment_PostID",
            columnList = "PostID"
        )
    )
    public static class PostComment {

        @Id
        @Column(name = "PostCommentID")
        private Long id;

        @Column(name = "Review")
        private String review;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "PostID")
        private Post post;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getReview() {
            return review;
        }

        public void setReview(String review) {
            this.review = review;
        }

        public Post getPost() {
            return post;
        }

        public void setPost(Post post) {
            this.post = post;
        }
    }
}

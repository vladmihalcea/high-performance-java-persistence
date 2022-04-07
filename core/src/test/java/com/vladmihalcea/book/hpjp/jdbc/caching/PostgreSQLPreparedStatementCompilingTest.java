package com.vladmihalcea.book.hpjp.jdbc.caching;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.*;
import org.hibernate.Session;
import org.junit.Test;
import org.postgresql.PGStatement;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLPreparedStatementCompilingTest extends AbstractTest {

    public static final String INSERT_POST = "INSERT INTO post (id, title, status) VALUES (?, ?, ?)";

    @Override
    protected Class<?>[] entities() {
        return new Class[] {
            Post.class
        };
    }

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Test
    public void testInsert() {
        AtomicInteger statementCount = new AtomicInteger();
        long startNanos = System.nanoTime();
        doInJDBC(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_POST)) {
                int postCount = getPostCount();

                for (int i = 1; i <= postCount; i++) {
                    PostStatus status = PostStatus.APPROVED;
                    if (i > postCount * 0.99) {
                        status = PostStatus.SPAM;
                    } else if (i > postCount * 0.95) {
                        status = PostStatus.PENDING;
                    }
                    statement.setLong(1, i);
                    statement.setString(2, String.format("High-Performance Java Persistence, page %d", i));
                    statement.setInt(3, status.ordinal());

                    executeStatement(statement, statementCount);
                }
                statement.executeBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
        LOGGER.info("{}.testInsert took {} millis",
            getClass().getSimpleName(),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
        );

        /*doInJDBC(connection -> {
            //c:\Program Files\PostgreSQL\13\data\log\postgresql-yyyy-MM-dd_hhmmss.log
            executeStatement(
                connection,
                "LOAD 'auto_explain'",
                "SET auto_explain.log_analyze=true",
                "SET auto_explain.log_min_duration=0"
            );

            try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM post WHERE status = ?")) {
                postIdsForStatus(statement, PostStatus.PENDING);
                postIdsForStatus(statement, PostStatus.APPROVED);
                postIdsForStatus(statement, PostStatus.SPAM);
                postIdsForStatus(statement, PostStatus.PENDING);
                postIdsForStatus(statement, PostStatus.APPROVED);
                postIdsForStatus(statement, PostStatus.SPAM);
                postIdsForStatus(statement, PostStatus.PENDING);
            }
        });*/

        doInJPA(entityManager -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < 10; i++) {
                List<String> planLines = entityManager.createNativeQuery("""
                    EXPLAIN ANALYZE
                    SELECT *
                    FROM post p
                    WHERE p.status = :status
                    """)
                    .setParameter("status", random.nextInt(PostStatus.values().length))
                    .getResultList();

                LOGGER.info("Execution plan: {}{}",
                    System.lineSeparator(),
                    planLines.stream().collect(Collectors.joining(System.lineSeparator()))
                );
            }
        });
    }

    protected List<Long> postIdsForStatus(PreparedStatement statement, PostStatus status)
            throws SQLException {
        List<Long> ids = new ArrayList<>();

        statement.setInt(1, status.ordinal());
        try(ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getLong(1));
            }
        }

        return ids;
    }

    public void setPrepareThreshold(Statement statement, int threshold) throws SQLException {
        if(statement instanceof PGStatement) {
            PGStatement pgStatement = (PGStatement) statement;
            pgStatement.setPrepareThreshold(threshold);
        } else {
            InvocationHandler handler = Proxy.getInvocationHandler(statement);
            try {
                handler.invoke(statement, PGStatement.class.getMethod("setPrepareThreshold", int.class), new Object[]{threshold});
            } catch (Throwable throwable) {
                throw new IllegalArgumentException(throwable);
            }
        }
    }

    private void executeStatement(PreparedStatement statement, AtomicInteger statementCount) throws SQLException {
        statement.addBatch();
        int count = statementCount.incrementAndGet();
        if(count % getBatchSize() == 0) {
            statement.executeBatch();
        }
    }

    protected int getPostCount() {
        return 1000;
    }

    protected int getBatchSize() {
        return 100;
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

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

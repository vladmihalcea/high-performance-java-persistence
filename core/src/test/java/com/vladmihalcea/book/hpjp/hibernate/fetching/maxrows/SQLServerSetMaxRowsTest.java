package com.vladmihalcea.book.hpjp.hibernate.fetching.maxrows;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.AbstractSQLServerIntegrationTest;
import org.hibernate.Session;
import org.hibernate.annotations.CreationTimestamp;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SQLServerSetMaxRowsTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{
            Post.class
        };
    }

    @Override
    public void afterInit() {
        doInJPA(entityManager -> {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("CREATE INDEX idx_post_created_on ON post (created_on DESC)");
                }
            });
            LongStream.range(0, 50 * 100).forEach(i -> {
                Post post = new Post(i);
                post.setTitle(String.format("Post nr. %d", i));
                entityManager.persist(post);
                if (i % 50 == 0 && i > 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            });
        });
    }

    @Override
    protected void additionalProperties(Properties properties) {
        properties.put("hibernate.jdbc.batch_size", "50");
        properties.put("hibernate.order_inserts", "true");
        properties.put("hibernate.order_updates", "true");
    }

    @Test
    public void testSetMaxSize() {
        doInJPA(entityManager -> {
            List<Map<String, String>> planLines = new ArrayList<>();

            entityManager.unwrap(Session.class)
                .doWork(connection -> {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(
                            "SET STATISTICS IO, TIME, PROFILE ON"
                        );

                        statement.setMaxRows(50);
                        boolean moreResultSets = statement.execute("""
                            SELECT p.title
                            FROM post p
                            ORDER BY p.created_on DESC
                            """);

                        while (moreResultSets) {
                            planLines.addAll(parseResultSet(statement.getResultSet()));

                            moreResultSets = statement.getMoreResults();
                        }

                        statement.executeUpdate(
                            "SET STATISTICS IO, TIME, PROFILE OFF"
                        );
                    }
                });

            LOGGER.info("Execution plan: {}{}",
                System.lineSeparator(),
                planLines.stream().map(Map::toString).collect(Collectors.joining(System.lineSeparator()))
            );
        });
    }

    @Test
    public void testLimit() {
        doInJPA(entityManager -> {
            List<Map<String, String>> planLines = new ArrayList<>();

            entityManager.unwrap(Session.class)
                .doWork(connection -> {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(
                            "SET STATISTICS IO, TIME, PROFILE ON"
                        );

                        boolean moreResultSets = statement.execute("""
                            SELECT TOP 50 p.title
                            FROM post p
                            ORDER BY p.created_on DESC
                            """);

                        while (moreResultSets) {
                            planLines.addAll(parseResultSet(statement.getResultSet()));

                            moreResultSets = statement.getMoreResults();
                        }

                        statement.executeUpdate(
                            "SET STATISTICS IO, TIME, PROFILE OFF"
                        );
                    }
                });

            LOGGER.info("Execution plan: {}{}",
                System.lineSeparator(),
                planLines.stream().map(Map::toString).collect(Collectors.joining(System.lineSeparator()))
            );
        });
    }

    @Entity(name = "Post")
    @Table(name = "post")
    public static class Post {

        @Id
        private Long id;

        private String title;

        @Column(name = "created_on")
        @CreationTimestamp
        private Date createdOn;

        public Post() {
        }

        public Post(Long id) {
            this.id = id;
        }

        public Post(String title) {
            this.title = title;
        }

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
}

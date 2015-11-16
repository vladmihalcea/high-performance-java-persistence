package com.vladmihalcea.book.hpjp.jdbc.batch.generatedkeys.identity;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import java.sql.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GeneratedKeysBatchPreparedStatementTest - Base class for testing JDBC PreparedStatement generated keys
 *
 * @author Vlad Mihalcea
 */
public class PostgreSQLGeneratedKeysBatchPreparedStatementTest extends AbstractPostgreSQLIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Test
    public void testBatch() {
        doInJDBC(this::batchInsert);
    }

    protected int getPostCount() {
        return 10;
    }

    protected int getBatchSize() {
        return 5;
    }

    protected void batchInsert(Connection connection) throws SQLException {
        LOGGER.info("Identity generated keys for PostgreSQL");

        try(Statement statement = connection.createStatement()) {
            statement.executeUpdate("drop table if exists post cascade");

            statement.executeUpdate(
                "create table post (" +
                "    id bigserial not null, " +
                "    title varchar(255), " +
                "    version int4 not null, " +
                "    primary key (id))"
            );
        }

        AtomicInteger postStatementCount = new AtomicInteger();

        try (PreparedStatement postStatement = connection.prepareStatement("insert into post (title, version) values (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            int postCount = getPostCount();

            int index;

            postStatement.setString(1, String.format("Post no. %1$d", -1));
            postStatement.setInt(2, 0);
            postStatement.executeUpdate();

            try (ResultSet resultSet = postStatement.getGeneratedKeys()) {
                while (resultSet.next()) {
                    LOGGER.info("Generated identifier: {}", resultSet.getLong(1));
                }
            }

            for (int i = 0; i < postCount; i++) {
                index = 0;

                postStatement.setString(++index, String.format("Post no. %1$d", i));
                postStatement.setInt(++index, 0);
                postStatement.addBatch();
                int count = postStatementCount.incrementAndGet();
                if (count % getBatchSize() == 0) {
                    postStatement.executeBatch();
                    try (ResultSet resultSet = postStatement.getGeneratedKeys()) {
                        while (resultSet.next()) {
                            LOGGER.info("Generated identifier: {}", resultSet.getLong(1));
                        }
                    }
                }
            }
        }
    }
}
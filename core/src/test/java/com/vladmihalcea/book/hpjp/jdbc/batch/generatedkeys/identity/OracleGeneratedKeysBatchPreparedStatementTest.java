package com.vladmihalcea.book.hpjp.jdbc.batch.generatedkeys.identity;

import com.vladmihalcea.book.hpjp.util.AbstractOracleXEIntegrationTest;
import org.junit.Test;

import java.sql.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GeneratedKeysBatchPreparedStatementTest - Base class for testing JDBC PreparedStatement generated keys
 *
 * @author Vlad Mihalcea
 */
public class OracleGeneratedKeysBatchPreparedStatementTest extends AbstractOracleXEIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Test
    public void testBatch() throws SQLException {
        doInJDBC(this::batchInsert);
    }

    protected int getPostCount() {
        return 10;
    }

    protected int getBatchSize() {
        return 5;
    }

    protected void batchInsert(Connection connection) throws SQLException {
        LOGGER.info("Identity generated keys for Oracle");

        try(Statement statement = connection.createStatement()) {
            statement.executeUpdate("drop sequence post_seq");
        } catch (Exception ignore) {}

        try(Statement statement = connection.createStatement()) {
            statement.executeUpdate("drop table post");
        } catch (Exception ignore) {}

        try(Statement statement = connection.createStatement()) {

            statement.executeUpdate(
                "CREATE SEQUENCE post_seq"
            );

            statement.executeUpdate(
                "create table post (" +
                "    id number(19,0) not null, " +
                "    title varchar2(255 char), " +
                "    version number(10,0) not null, " +
                "    primary key (id))"
            );

            statement.executeUpdate(
                "create or replace trigger post_identity" +
                "   before insert on post " +
                "   for each row" +
                "   begin" +
                "       select post_seq.nextval" +
                "       into   :new.id" +
                "       from   dual;" +
                "end;"
            );
        }

        AtomicInteger postStatementCount = new AtomicInteger();

        try (PreparedStatement postStatement = connection.prepareStatement("insert into post (title, version) values (?, ?)", new int[]{1})) {
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
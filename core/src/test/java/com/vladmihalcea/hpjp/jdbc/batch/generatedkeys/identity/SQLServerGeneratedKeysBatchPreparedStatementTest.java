package com.vladmihalcea.hpjp.jdbc.batch.generatedkeys.identity;

import com.vladmihalcea.hpjp.util.AbstractSQLServerIntegrationTest;
import org.hibernate.exception.GenericJDBCException;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * GeneratedKeysBatchPreparedStatementTest - Base class for testing JDBC PreparedStatement generated keys
 *
 * @author Vlad Mihalcea
 */
public class SQLServerGeneratedKeysBatchPreparedStatementTest extends AbstractSQLServerIntegrationTest {

    @Override
    protected Class<?>[] entities() {
        return new Class[]{};
    }

    @Test
    public void testBatch() {
        assertThrows(GenericJDBCException.class, ()->{
            doInJDBC(this::batchInsert);
        });
    }

    protected int getPostCount() {
        return 10;
    }

    protected int getBatchSize() {
        return 5;
    }

    protected void batchInsert(Connection connection) throws SQLException {
        LOGGER.info("Identity generated keys for SQL Server");

        executeStatement(connection, true, "drop table post");
        executeStatement(connection, true, """
            create table post (
                id bigint identity not null,
                title varchar(255),
                version int not null,
                primary key (id)
            )
            """);

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
                    int[] updateCount = postStatement.executeBatch();
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
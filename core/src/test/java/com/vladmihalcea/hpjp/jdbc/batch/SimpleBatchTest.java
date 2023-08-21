package com.vladmihalcea.hpjp.jdbc.batch;

import com.vladmihalcea.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.hpjp.util.AbstractTest;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SimpleBatchTest extends AbstractTest {

    private BlogEntityProvider blogEntityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return blogEntityProvider.entities();
    }

    @Override
    protected Database database() {
        return Database.SQLSERVER;
    }

    @Test
    public void testNoBatching() {
        LOGGER.info("Test Statement batch insert");
        doInJDBC(connection -> {
            try (Statement statement = connection.createStatement()) {
                int rowCount = statement.executeUpdate("""
                    INSERT INTO post (title, version, id)
                    VALUES ('Post no. 1', 0, 1)
                    """);

                assertEquals(1, rowCount);

                rowCount = statement.executeUpdate("""
                    INSERT INTO post (title, version, id)
                    VALUES ('Post no. 2', 0, 2)
                    """);

                assertEquals(1, rowCount);
            }
        });
    }

    @Test
    public void testStatement() {
        LOGGER.info("Test Statement batch insert");
        doInJDBC(connection -> {
            try (Statement statement = connection.createStatement()) {

                statement.addBatch("""
                    INSERT INTO post (title, version, id)
                    VALUES ('Post no. 1', 0, 1)
                    """);

                statement.addBatch("""
                    INSERT INTO post (title, version, id)
                    VALUES ('Post no. 2', 0, 2)
                    """);

                int[] updateCounts = statement.executeBatch();
                assertEquals(2, updateCounts.length);
                assertEquals(1, updateCounts[0]);
                assertEquals(1, updateCounts[1]);
            }
        });
    }

    @Test
    public void testPreparedStatement() {
        LOGGER.info("Test Statement batch insert");
        doInJDBC(connection -> {
            try (PreparedStatement postStatement = connection.prepareStatement("""
                INSERT INTO post (title, version, id)
                VALUES (?, ?, ?)
                """)
            ) {

                postStatement.setString(1, String.format("Post no. %1$d", 1));
                postStatement.setInt(2, 0);
                postStatement.setLong(3, 1);
                postStatement.addBatch();

                postStatement.setString(1, String.format("Post no. %1$d", 2));
                postStatement.setInt(2, 0);
                postStatement.setLong(3, 2);
                postStatement.addBatch();

                int[] updateCounts = postStatement.executeBatch();

                assertEquals(2, updateCounts.length);
            }
        });
    }
}

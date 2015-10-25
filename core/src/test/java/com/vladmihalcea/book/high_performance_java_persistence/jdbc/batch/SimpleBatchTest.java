package com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch;

import com.vladmihalcea.book.high_performance_java_persistence.util.providers.BatchEntityProvider;
import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractPostgreSQLIntegrationTest;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

/**
 * <code>SimpleBatchStatementTest</code> - Simple Batch StatementTest
 *
 * @author Vlad Mihalcea
 */
public class SimpleBatchTest extends AbstractPostgreSQLIntegrationTest {

    private BatchEntityProvider batchEntityProvider = new BatchEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return batchEntityProvider.entities();
    }

    @Test
    public void testStatement() {
        LOGGER.info("Test Statement batch insert");
        doInConnection(connection -> {
            try (Statement statement = connection.createStatement()) {

                statement.addBatch(
                    "insert into post (title, version, id) " +
                    "values ('Post no. 1', 0, 1)");

                statement.addBatch(
                    "insert into post_comment (post_id, review, version, id) " +
                    "values (1, 'Post comment 1.1', 0, 1)");
                statement.addBatch(
                    "insert into post_comment (post_id, review, version, id) " +
                    "values (1, 'Post comment 1.2', 0, 2)");

                int[] updateCounts = statement.executeBatch();

                assertEquals(3, updateCounts.length);
            }
        });
    }

    @Test
    public void testPreparedStatement() {
        LOGGER.info("Test Statement batch insert");
        doInConnection(connection -> {
            PreparedStatement postStatement = connection.prepareStatement(
                    "insert into post (title, version, id) " +
                    "values (?, ?, ?)");

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

            postStatement.close();
        });
    }
}

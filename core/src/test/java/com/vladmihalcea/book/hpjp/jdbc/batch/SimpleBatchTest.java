package com.vladmihalcea.book.hpjp.jdbc.batch;

import com.vladmihalcea.book.hpjp.util.AbstractPostgreSQLIntegrationTest;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class SimpleBatchTest extends AbstractPostgreSQLIntegrationTest {

    private BlogEntityProvider blogEntityProvider = new BlogEntityProvider();

    @Override
    protected Class<?>[] entities() {
        return blogEntityProvider.entities();
    }

    @Test
    public void testStatement() {
        LOGGER.info("Test Statement batch insert");
        doInJDBC(connection -> {
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
        doInJDBC(connection -> {
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

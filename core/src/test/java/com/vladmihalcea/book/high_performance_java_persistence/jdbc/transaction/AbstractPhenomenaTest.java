package com.vladmihalcea.book.high_performance_java_persistence.jdbc.transaction;

import com.vladmihalcea.book.high_performance_java_persistence.jdbc.batch.providers.BatchEntityProvider;
import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * PhenomenaTest - Test to validate what phenomena does a certain isolation level prevents
 *
 * @author Vlad Mihalcea
 */
@RunWith(Parameterized.class)
public abstract class AbstractPhenomenaTest extends AbstractTest {

    public static final String INSERT_POST = "insert into post (title, version, id) values (?, ?, ?)";

    public static final String INSERT_POST_COMMENT = "insert into post_comment (post_id, review, version, id) values (?, ?, ?, ?)";

    public static final String INSERT_POST_DETAILS = "insert into post_details (id, created_on, version) values (?, ?, ?)";

    private final String isolationLevelName;

    private final int isolationLevel;

    private final CountDownLatch bobLatch = new CountDownLatch(1);

    private BatchEntityProvider entityProvider = new BatchEntityProvider();

    protected AbstractPhenomenaTest(String isolationLevelName, int isolationLevel) {
        this.isolationLevelName = isolationLevelName;
        this.isolationLevel = isolationLevel;
    }

    public String getIsolationLevelName() {
        return isolationLevelName;
    }

    public int getIsolationLevel() {
        return isolationLevel;
    }

    @Override
    protected Class<?>[] entities() {
        return entityProvider.entities();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        List<Object[]> levels = new ArrayList<>();
        levels.add(new Object[]{"Read Uncommitted", Connection.TRANSACTION_READ_UNCOMMITTED});
        levels.add(new Object[]{"Read Committed", Connection.TRANSACTION_READ_COMMITTED});
        levels.add(new Object[]{"Repeatable Read", Connection.TRANSACTION_REPEATABLE_READ});
        levels.add(new Object[]{"Serializable", Connection.TRANSACTION_SERIALIZABLE});
        return levels;
    }

    @Override
    public void init() {
        super.init();
        doInConnection(connection -> {
            try (
                    PreparedStatement postStatement = connection.prepareStatement(INSERT_POST);
                    PreparedStatement postCommentStatement = connection.prepareStatement(INSERT_POST_COMMENT);
                    PreparedStatement postDetailsStatement = connection.prepareStatement(INSERT_POST_DETAILS);
            ) {
                int index = 0;
                postStatement.setString(++index, "Transactions");
                postStatement.setInt(++index, 0);
                postStatement.setLong(++index, 1);
                postStatement.executeUpdate();

                index = 0;
                postDetailsStatement.setInt(++index, 1);
                postDetailsStatement.setTimestamp(++index, new Timestamp(System.currentTimeMillis()));
                postDetailsStatement.setInt(++index, 0);
                postDetailsStatement.executeUpdate();

                index = 0;
                postCommentStatement.setLong(++index, 1);
                postCommentStatement.setString(++index, String.format("Post comment %1$d", 1));
                postCommentStatement.setInt(++index, (int) (Math.random() * 1000));
                postCommentStatement.setLong(++index, 1);
                postCommentStatement.addBatch();
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testDirtyRead() {
        final AtomicReference<Boolean> dirtyRead = new AtomicReference<>(false);

        doInConnection(aliceConnection -> {
            if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                LOGGER.info("Database {} doesn't support {}", getDataSourceProvider().database(), isolationLevelName);
                return;
            }
            prepareConnection(aliceConnection);
            try (Statement postUpdate = aliceConnection.createStatement()) {
                postUpdate.executeUpdate("UPDATE post SET title = 'ACID' WHERE id = 1");
                executeAsync(() -> {
                    doInConnection(bobConnection -> {
                        prepareConnection(bobConnection);
                        try (Statement postSelect = bobConnection.createStatement();
                             ResultSet postResultSet = postSelect.executeQuery(dirtyReadSql())) {
                            assertTrue(postResultSet.next());
                            String title = postResultSet.getString(1);
                            if ("Transactions".equals(title)) {
                                LOGGER.info("No Dirty Read, uncommitted data is not viewable");
                            } else if ("ACID".equals(title)) {
                                dirtyRead.set(true);
                            } else {
                                fail("Unknown title: " + title);
                            }
                        } catch (SQLException e) {
                            fail(e.getMessage());
                        } finally {
                            bobLatch.countDown();
                        }
                    });
                });
                awaitOnLatch(bobLatch);
            }
            LOGGER.info("Isolation level {} {} Dirty Reads", isolationLevelName, dirtyRead.get() ? "allows" : "prevents");
        });
    }

    protected void prepareConnection(Connection connection) throws SQLException {
        connection.setTransactionIsolation(isolationLevel);
    }

    protected String dirtyReadSql() {
        return "SELECT title FROM post WHERE id = 1";
    }

}

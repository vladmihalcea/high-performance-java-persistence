package com.vladmihalcea.book.high_performance_java_persistence.jdbc.transaction;

import com.vladmihalcea.book.high_performance_java_persistence.util.providers.BatchEntityProvider;
import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
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

    public static final String INSERT_POST_DETAILS = "insert into post_details (id, created_by, version) values (?, ?, ?)";

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
                postDetailsStatement.setString(++index, "None");
                postDetailsStatement.setInt(++index, 0);
                postDetailsStatement.executeUpdate();

                for(int i = 0; i < 3; i++) {
                    index = 0;
                    postCommentStatement.setLong(++index, 1);
                    postCommentStatement.setString(++index, String.format("Post comment %1$d", 1));
                    postCommentStatement.setInt(++index, (int) (Math.random() * 1000));
                    postCommentStatement.setLong(++index, i);
                    postCommentStatement.executeUpdate();
                }
            } catch (SQLException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void testDirtyWrite() {
        String firstTitle = "Alice";
        try {
            doInConnection(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", getDataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                update(aliceConnection, updatePostTitleParamSql(), new Object[]{firstTitle});
                try {
                    executeSync(() -> {
                        doInConnection(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                update(bobConnection, updatePostTitleParamSql(), new Object[]{"Bob"});
                            } catch (Exception e) {
                                LOGGER.info("Exception thrown", e);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.info("Exception thrown", e);
                }
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
        }
        doInConnection(aliceConnection -> {
            String title = selectStringColumn(aliceConnection, selectPostTitleSql());
            LOGGER.info("Isolation level {} {} Dirty Write", isolationLevelName, !title.equals(firstTitle) ? "allows" : "prevents");
        });
    }

    @Test
    public void testDirtyRead() {
        final AtomicBoolean dirtyRead = new AtomicBoolean();

        try {
            doInConnection(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", getDataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                try (Statement aliceStatement = aliceConnection.createStatement()) {
                    aliceStatement.executeUpdate(updatePostTitleSql());
                    executeSync(() -> {
                        doInConnection(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                String title = selectStringColumn(bobConnection, selectPostTitleSql());
                                if ("Transactions".equals(title)) {
                                    LOGGER.info("No Dirty Read, uncommitted data is not viewable");
                                } else if ("ACID".equals(title)) {
                                    dirtyRead.set(true);
                                } else {
                                    fail("Unknown title: " + title);
                                }
                            } catch (Exception e) {
                                LOGGER.info("Exception thrown", e);
                            }
                        });
                    });
                }
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
        }
        LOGGER.info("Isolation level {} {} Dirty Reads", isolationLevelName, dirtyRead.get() ? "allows" : "prevents");
    }

    @Test
    public void testNonRepeatableRead() {
        doInConnection(aliceConnection -> {
            if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                LOGGER.info("Database {} doesn't support {}", getDataSourceProvider().database(), isolationLevelName);
                return;
            }
            prepareConnection(aliceConnection);
            String firstTitle = selectStringColumn(aliceConnection, selectPostTitleSql());
            try {
                executeSync(() -> {
                    doInConnection(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            assertEquals(1, update(bobConnection, updatePostTitleSql()));
                        } catch (Exception e) {
                            LOGGER.info("Exception thrown", e);
                        }
                    });
                });
            } catch (Exception e) {
                LOGGER.info("Exception thrown", e);
            }
            String secondTitle = selectStringColumn(aliceConnection, selectPostTitleSql());

            LOGGER.info("Isolation level {} {} Non-Repeatable Reads", isolationLevelName, !firstTitle.equals(secondTitle) ? "allows" : "prevents");
        });
    }

    @Test
    public void testPhantomRead() {
        doInConnection(aliceConnection -> {
            if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                LOGGER.info("Database {} doesn't support {}", getDataSourceProvider().database(), isolationLevelName);
                return;
            }
            prepareConnection(aliceConnection);
            int commentsCount = count(aliceConnection, countCommentsSql());
            try {
                executeSync(() -> {
                    doInConnection(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            assertEquals(1, update(bobConnection, insertCommentSql()));
                        } catch (Exception e) {
                            LOGGER.info("Exception thrown", e);
                        }
                    });
                });
            } catch (Exception e) {
                LOGGER.info("Exception thrown", e);
            }
            int secondCommentsCount = count(aliceConnection, countCommentsSql());

            LOGGER.info("Isolation level {} {} Phantom Reads", isolationLevelName, secondCommentsCount != commentsCount ? "allows" : "prevents");
        });
    }

    @Test
    public void testLostUpdate() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
        try {
            doInConnection(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", getDataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                String title = selectStringColumn(aliceConnection, selectPostTitleSql());
                executeSync(() -> {
                    doInConnection(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            update(bobConnection, updatePostTitleParamSql(), new Object[]{"Bob"});
                        } catch (Exception e) {
                            LOGGER.info("Exception thrown", e);
                            preventedByLocking.set(true);
                        }
                    });
                });
                update(aliceConnection, updatePostTitleParamSql(), new Object[]{"Alice"});
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            preventedByLocking.set(true);
        }
        doInConnection(aliceConnection -> {
            String title = selectStringColumn(aliceConnection, selectPostTitleSql());
            if(Boolean.TRUE.equals(preventedByLocking.get())) {
                LOGGER.info("Isolation level {} Lost Update prevented by locking", isolationLevelName);
            } else {
                LOGGER.info("Isolation level {} {} Lost Update", isolationLevelName, "Alice".equals(title) ? "allows" : "prevents");
            }
        });
    }

    @Test
    public void testReadSkew() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
        try {
            doInConnection(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", getDataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                String title = selectStringColumn(aliceConnection, selectPostTitleSql());

                executeSync(() -> {
                    doInConnection(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            update(bobConnection, updatePostTitleParamSql(), new Object[]{"Bob"});
                            update(bobConnection, updatePostDetailsAuthorParamSql(), new Object[]{"Bob"});
                        } catch (Exception e) {
                            LOGGER.info("Exception thrown", e);
                            preventedByLocking.set(true);
                        }
                    });
                });
                String createdBy = selectStringColumn(aliceConnection, selectPostDetailsAuthorSql());
                LOGGER.info("Isolation level {} {} Read Skew", isolationLevelName, "Bob".equals(createdBy) ? "allows" : "prevents");
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            preventedByLocking.set(true);
        }
        doInConnection(aliceConnection -> {
            String title = selectStringColumn(aliceConnection, selectPostTitleSql());
            if(Boolean.TRUE.equals(preventedByLocking.get())) {
                LOGGER.info("Isolation level {} Read Skew prevented by locking", isolationLevelName);
            }
        });
    }

    @Test
    public void testWriteSkew() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
        try {
            doInConnection(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", getDataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                String title = selectStringColumn(aliceConnection, selectPostTitleSql());
                String createdBy = selectStringColumn(aliceConnection, selectPostDetailsAuthorSql());

                executeSync(() -> {
                    doInConnection(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            String bobTitle = selectStringColumn(bobConnection, selectPostTitleSql());
                            String bonCreatedBy = selectStringColumn(bobConnection, selectPostDetailsAuthorSql());
                            update(bobConnection, updatePostTitleParamSql(), new Object[]{"Bob"});
                        } catch (Exception e) {
                            LOGGER.info("Exception thrown", e);
                            preventedByLocking.set(true);
                        }
                    });
                });
                update(aliceConnection, updatePostDetailsAuthorParamSql(), new Object[]{"Alice"});
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            preventedByLocking.set(true);
        }
        LOGGER.info("Isolation level {} {} Write Skew", isolationLevelName, !Boolean.TRUE.equals(preventedByLocking.get()) ? "allows" : "prevents");
    }

    protected void prepareConnection(Connection connection) throws SQLException {
        connection.setTransactionIsolation(isolationLevel);
        try {
            connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), 1000);
        } catch (Throwable e) {
            LOGGER.info("Unsupported operation", e);
        }
    }

    protected String selectPostTitleSql() {
        return "SELECT title FROM post WHERE id = 1";
    }

    protected String selectPostDetailsAuthorSql() {
        return "SELECT created_by FROM post_details WHERE id = 1";
    }

    protected String updatePostTitleSql() {
        return "UPDATE post SET title = 'ACID' WHERE id = 1";
    }

    protected String updatePostTitleParamSql() {
        return "UPDATE post SET title = ? WHERE id = 1";
    }

    protected String updatePostDetailsAuthorParamSql() {
        return "UPDATE post_details SET created_by = ? WHERE id = 1";
    }

    protected String countCommentsSql() {
        return "SELECT COUNT(*) FROM post_comment";
    }

    protected String insertCommentSql() {
        return "INSERT INTO post_comment (post_id, review, version, id) VALUES (1, 'Phantom', 0, 1000)";
    }

}

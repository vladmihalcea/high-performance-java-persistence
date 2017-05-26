package com.vladmihalcea.book.hpjp.jdbc.transaction.phenomena;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import com.vladmihalcea.book.hpjp.util.providers.entity.BlogEntityProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.persistence.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
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

    public static final String INSERT_DEPARTMENT = "insert into department (name, budget, id) values (?, ?, ?)";

    public static final String INSERT_EMPLOYEE = "insert into employee (department_id, name, salary, id) values (?, ?, ?, ?)";

    protected final String isolationLevelName;

    protected final int isolationLevel;

    private final CountDownLatch bobLatch = new CountDownLatch(1);

    private BlogEntityProvider entityProvider = new BlogEntityProvider();

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
        doInJDBC(connection -> {
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

                for (int i = 0; i < 3; i++) {
                    index = 0;
                    postCommentStatement.setLong(++index, 1);
                    postCommentStatement.setString(++index, String.format("Post comment %1$d", i));
                    postCommentStatement.setInt(++index, 0);
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
        final AtomicBoolean preventedByLocking = new AtomicBoolean();

        doInJDBC(aliceConnection -> {
            if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                return;
            }
            prepareConnection(aliceConnection);
            update(aliceConnection, updatePostTitleParamSql(), new Object[]{firstTitle});
            try {
                executeSync(() -> {
                    doInJDBC(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            update(bobConnection, updatePostTitleParamSql(), new Object[]{"Bob"});
                        } catch (Exception e) {
                            if( ExceptionUtil.isLockTimeout( e )) {
                                preventedByLocking.set( true );
                            } else {
                                throw new IllegalStateException( e );
                            }
                        }
                    });
                });
            } catch (Exception e) {
                if ( !ExceptionUtil.isConnectionClose( e ) ) {
                    fail(e.getMessage());
                }
            }
        });

        doInJDBC(aliceConnection -> {
            String title = selectStringColumn(aliceConnection, selectPostTitleSql());
            LOGGER.info("Isolation level {} {} Dirty Write", isolationLevelName, !title.equals(firstTitle) ? "allows" : "prevents");
            if(preventedByLocking.get()) {
                LOGGER.info("Isolation level {} prevents Dirty Write by locking", isolationLevelName);
            }
        });
    }

    @Test
    public void testDirtyRead() {
        final AtomicBoolean dirtyRead = new AtomicBoolean();
        final AtomicBoolean preventedByLocking = new AtomicBoolean();

        doInJDBC(aliceConnection -> {
            if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                return;
            }
            prepareConnection(aliceConnection);
            try (Statement aliceStatement = aliceConnection.createStatement()) {
                aliceStatement.executeUpdate(updatePostTitleSql());
                executeSync(() -> {
                    doInJDBC(bobConnection -> {
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
                            if( ExceptionUtil.isLockTimeout( e )) {
                                preventedByLocking.set( true );
                            } else {
                                throw new IllegalStateException( e );
                            }
                        }
                    });
                });
            }
        });

        LOGGER.info("Isolation level {} {} Dirty Read", isolationLevelName, dirtyRead.get() ? "allows" : "prevents");
        if(preventedByLocking.get()) {
            LOGGER.info("Isolation level {} prevents Dirty Read by locking", isolationLevelName);
        }
    }

    @Test
    public void testNonRepeatableRead() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();

        doInJDBC(aliceConnection -> {
            if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                return;
            }
            prepareConnection(aliceConnection);
            String firstTitle = selectStringColumn(aliceConnection, selectPostTitleSql());
            try {
                executeSync(() -> {
                    doInJDBC(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            assertEquals(1, update(bobConnection, updatePostTitleSql()));
                        } catch (Exception e) {
                            if( ExceptionUtil.isLockTimeout( e )) {
                                preventedByLocking.set( true );
                            } else {
                                throw new IllegalStateException( e );
                            }
                        }
                    });
                });
            } catch (Exception e) {
                if ( !ExceptionUtil.isConnectionClose( e ) ) {
                    fail(e.getMessage());
                }
            }
            String secondTitle = selectStringColumn(aliceConnection, selectPostTitleSql());

            LOGGER.info("Isolation level {} {} Non-Repeatable Read", isolationLevelName, !firstTitle.equals(secondTitle) ? "allows" : "prevents");
            if(preventedByLocking.get()) {
                LOGGER.info("Isolation level {} prevents Non-Repeatable Read by locking", isolationLevelName);
            }
        });
    }

    @Test
    public void testPhantomRead() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();

        doInJDBC(aliceConnection -> {
            if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                return;
            }
            prepareConnection(aliceConnection);
            int commentsCount = count(aliceConnection, countCommentsSql());
            assertEquals(3, commentsCount);
            update(aliceConnection, updateCommentsSql());
            try {
                executeSync(() -> {
                    doInJDBC(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            assertEquals(1, update(bobConnection, insertCommentSql()));
                        } catch (Exception e) {
                            if( ExceptionUtil.isLockTimeout( e )) {
                                preventedByLocking.set( true );
                            } else {
                                throw new IllegalStateException( e );
                            }
                        }
                    });
                });
            } catch (Exception e) {
                if ( !ExceptionUtil.isConnectionClose( e ) ) {
                    fail(e.getMessage());
                }
            }
            int secondCommentsCount = count(aliceConnection, countCommentsSql());

            LOGGER.info("Isolation level {} {} Phantom Reads", isolationLevelName, secondCommentsCount != commentsCount ? "allows" : "prevents");
            if(preventedByLocking.get()) {
                LOGGER.info("Isolation level {} prevents Phantom Read by locking", isolationLevelName);
            }
        });
    }

    @Test
    public void testLostUpdate() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();
        final AtomicBoolean preventedByMVCC = new AtomicBoolean();
        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                String title = selectStringColumn(aliceConnection, selectPostTitleSql());
                executeSync(() -> {
                    doInJDBC(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            update(bobConnection, updatePostTitleParamSql(), new Object[]{"Bob"});
                        } catch (Exception e) {
                            if( ExceptionUtil.isLockTimeout( e )) {
                                preventedByLocking.set( true );
                            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                                preventedByMVCC.set( true );
                            } else {
                                throw new IllegalStateException( e );
                            }
                        }
                    });
                });
                update(aliceConnection, updatePostTitleParamSql(), new Object[]{"Alice"});
            });
        } catch (Exception e) {
            if( ExceptionUtil.isLockTimeout( e )) {
                preventedByLocking.set( true );
            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                preventedByMVCC.set( true );
            } else if ( !ExceptionUtil.isConnectionClose( e ) ) {
                throw new IllegalStateException( e );
            }
        }
        doInJDBC(aliceConnection -> {
            String title = selectStringColumn(aliceConnection, selectPostTitleSql());
            LOGGER.info("Isolation level {} {} Lost Update", isolationLevelName, "Alice".equals(title) ? "allows" : "prevents");

            if (Boolean.TRUE.equals(preventedByLocking.get())) {
                LOGGER.info("Isolation level {} prevents Lost Update by locking", isolationLevelName);
            } else if (Boolean.TRUE.equals(preventedByMVCC.get())) {
                LOGGER.info("Isolation level {} prevents Lost Update by MVCC", isolationLevelName);
            }
        });
    }

    @Test
    public void testReadSkew() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();
        final AtomicBoolean preventedByMVCC = new AtomicBoolean();
        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                String title = selectStringColumn(aliceConnection, selectPostTitleSql());

                executeSync(() -> {
                    doInJDBC(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            update(bobConnection, updatePostTitleParamSql(), new Object[]{"Bob"});
                            update(bobConnection, updatePostDetailsAuthorParamSql(), new Object[]{"Bob"});
                        } catch (Exception e) {
                            if( ExceptionUtil.isLockTimeout( e )) {
                                preventedByLocking.set( true );
                            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                                preventedByMVCC.set( true );
                            } else {
                                throw new IllegalStateException( e );
                            }
                        }
                    });
                });
                String createdBy = selectStringColumn(aliceConnection, selectPostDetailsAuthorSql());
                LOGGER.info("Isolation level {} {} Read Skew", isolationLevelName, "Bob".equals(createdBy) ? "allows" : "prevents");
            });
        } catch (Exception e) {
            if( ExceptionUtil.isLockTimeout( e )) {
                preventedByLocking.set( true );
            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                preventedByMVCC.set( true );
            } else if ( !ExceptionUtil.isConnectionClose( e ) ) {
                throw new IllegalStateException( e );
            }
        }
        doInJDBC(aliceConnection -> {
            String title = selectStringColumn(aliceConnection, selectPostTitleSql());

            if (Boolean.TRUE.equals(preventedByLocking.get())) {
                LOGGER.info("Isolation level {} prevents Read Skew by locking", isolationLevelName);
            } else if (Boolean.TRUE.equals(preventedByMVCC.get())) {
                LOGGER.info("Isolation level {} prevents Read Skew by MVCC", isolationLevelName);
            }
        });
    }

    @Test
    public void testWriteSkew() {
        final AtomicBoolean preventedByLocking = new AtomicBoolean();
        final AtomicBoolean preventedByMVCC = new AtomicBoolean();
        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                String title = selectStringColumn(aliceConnection, selectPostTitleSql());
                String createdBy = selectStringColumn(aliceConnection, selectPostDetailsAuthorSql());

                executeSync(() -> {
                    doInJDBC(bobConnection -> {
                        prepareConnection(bobConnection);
                        try {
                            String bobTitle = selectStringColumn(bobConnection, selectPostTitleSql());
                            String bonCreatedBy = selectStringColumn(bobConnection, selectPostDetailsAuthorSql());
                            update(bobConnection, updatePostTitleParamSql(), new Object[]{"Bob"});
                        } catch (Exception e) {
                            if( ExceptionUtil.isLockTimeout( e )) {
                                preventedByLocking.set( true );
                            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                                preventedByMVCC.set( true );
                            } else {
                                throw new IllegalStateException( e );
                            }
                        }
                    });
                });
                update(aliceConnection, updatePostDetailsAuthorParamSql(), new Object[]{"Alice"});
            });
        } catch (Exception e) {
            if( ExceptionUtil.isLockTimeout( e )) {
                preventedByLocking.set( true );
            } else if( ExceptionUtil.isMVCCAnomalyDetection( e )) {
                preventedByMVCC.set( true );
            } else {
                throw new IllegalStateException( e );
            }
        }
        if (Boolean.TRUE.equals(preventedByLocking.get())) {
            LOGGER.info("Isolation level {} prevents Write Skew by locking", isolationLevelName);
        } else if (Boolean.TRUE.equals(preventedByMVCC.get())) {
            LOGGER.info("Isolation level {} prevents Write Skew by MVCC", isolationLevelName);
        } else {
            LOGGER.info("Isolation level {} allows Write Skew", isolationLevelName);
        }
    }

    protected void prepareConnection(Connection connection) throws SQLException {
        connection.setTransactionIsolation(isolationLevel);
        setJdbcTimeout(connection);
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
        return "SELECT COUNT(*) FROM post_comment where post_id = 1";
    }

    protected String updateCommentsSql() {
        return "UPDATE post_comment SET version = 100 WHERE post_id = 1";
    }

    int nextId = 100;

    protected String insertCommentSql() {
        return String.format("INSERT INTO post_comment (post_id, review, version, id) VALUES (1, 'Phantom', 0, %d)", nextId++);
    }

}

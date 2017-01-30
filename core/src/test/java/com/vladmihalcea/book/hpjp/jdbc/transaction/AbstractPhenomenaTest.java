package com.vladmihalcea.book.hpjp.jdbc.transaction;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.BlogEntityProvider;
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
import java.util.concurrent.Executors;
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
        List<Class<?>> classes = new ArrayList<>(Arrays.asList(entityProvider.entities()));
        classes.add(Department.class);
        classes.add(Employee.class);
        return classes.toArray(new Class<?>[]{});
    }

    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        List<Object[]> levels = new ArrayList<>();
        /*levels.add(new Object[]{"Read Uncommitted", Connection.TRANSACTION_READ_UNCOMMITTED});
        levels.add(new Object[]{"Read Committed", Connection.TRANSACTION_READ_COMMITTED});
        levels.add(new Object[]{"Repeatable Read", Connection.TRANSACTION_REPEATABLE_READ});*/
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
        doInJDBC(connection -> {
            try (
                    PreparedStatement departmentStatement = connection.prepareStatement(INSERT_DEPARTMENT);
                    PreparedStatement employeeStatement = connection.prepareStatement(INSERT_EMPLOYEE);
            ) {
                int index = 0;
                departmentStatement.setString(++index, "Hypersistence");
                departmentStatement.setLong(++index, 100_000);
                departmentStatement.setLong(++index, 1);
                departmentStatement.executeUpdate();

                for (int i = 0; i < 3; i++) {
                    index = 0;
                    employeeStatement.setLong(++index, 1);
                    employeeStatement.setString(++index, String.format("John Doe %1$d", i));
                    employeeStatement.setLong(++index, 30_000);
                    employeeStatement.setLong(++index, i);
                    employeeStatement.executeUpdate();
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
        doInJDBC(aliceConnection -> {
            String title = selectStringColumn(aliceConnection, selectPostTitleSql());
            LOGGER.info("Isolation level {} {} Dirty Write", isolationLevelName, !title.equals(firstTitle) ? "allows" : "prevents");
        });
    }

    @Test
    public void testDirtyRead() {
        final AtomicBoolean dirtyRead = new AtomicBoolean();

        try {
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
        try {
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
                                //FOR MVCC
                                int _commentsCount = count(bobConnection, countCommentsSqlForInitialVersion());
                                assertEquals(3, _commentsCount);
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
        } catch (Exception e) {
            LOGGER.info("Exception thrown due to MVCC anomaly detection", e);
        }
    }

    @Test
    public void testLostUpdate() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
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
        doInJDBC(aliceConnection -> {
            String title = selectStringColumn(aliceConnection, selectPostTitleSql());
            if (Boolean.TRUE.equals(preventedByLocking.get())) {
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
        doInJDBC(aliceConnection -> {
            String title = selectStringColumn(aliceConnection, selectPostTitleSql());
            if (Boolean.TRUE.equals(preventedByLocking.get())) {
                LOGGER.info("Isolation level {} Read Skew prevented by locking", isolationLevelName);
            }
        });
    }

    @Test
    public void testWriteSkew() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
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

    @Test
    public void testPhantomReadAggregate() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
                assertEquals(90_000, salaryCount);

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                long _salaryCount = selectColumn(bobConnection, sumEmployeeSalarySql(), Number.class).longValue();
                                assertEquals(90_000, _salaryCount);

                                try (
                                        PreparedStatement employeeStatement = bobConnection.prepareStatement(insertEmployeeSql());
                                ) {
                                    int employeeId = 4;
                                    int index = 0;
                                    employeeStatement.setLong(++index, 1);
                                    employeeStatement.setString(++index, "Carol");
                                    employeeStatement.setLong(++index, 9_000);
                                    employeeStatement.setLong(++index, employeeId);
                                    employeeStatement.executeUpdate();
                                }
                            } catch (Exception e) {
                                LOGGER.info("Exception thrown", e);
                                preventedByLocking.set(true);
                                throw new IllegalStateException(e);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.info("Exception thrown", e);
                    preventedByLocking.set(true);
                }
                sleep(300);
                update(aliceConnection, "UPDATE employee SET salary = salary * 1.1 WHERE department_id = 1");
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            preventedByLocking.set(true);
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Read since the salary count is {} instead 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Read {}", isolationLevelName, preventedByLocking.get() ? "due to locking" : "");
            }
        });
    }

    @Test
    public void testPhantomReadAggregateWithInsert() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
                assertEquals(90_000, salaryCount);

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                long _salaryCount = selectColumn(bobConnection, sumEmployeeSalarySql(), Number.class).longValue();
                                assertEquals(90_000, _salaryCount);

                                try (
                                        PreparedStatement employeeStatement = bobConnection.prepareStatement(insertEmployeeSql());
                                ) {
                                    int employeeId = 4;
                                    int index = 0;
                                    employeeStatement.setLong(++index, 1);
                                    employeeStatement.setString(++index, "Carol");
                                    employeeStatement.setLong(++index, 9_000);
                                    employeeStatement.setLong(++index, employeeId);
                                    employeeStatement.executeUpdate();
                                }
                            } catch (Exception e) {
                                LOGGER.info("Exception thrown", e);
                                preventedByLocking.set(true);
                                throw new IllegalStateException(e);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.info("Exception thrown", e);
                    preventedByLocking.set(true);
                }
                sleep(300);
                try (
                        PreparedStatement employeeStatement = aliceConnection.prepareStatement(insertEmployeeSql());
                ) {
                    int employeeId = 5;
                    int index = 0;
                    employeeStatement.setLong(++index, 1);
                    employeeStatement.setString(++index, "Dave");
                    employeeStatement.setLong(++index, 9_000);
                    employeeStatement.setLong(++index, employeeId);
                    employeeStatement.executeUpdate();
                }
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            preventedByLocking.set(true);
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Read since the salary count is {} instead 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Read {}", isolationLevelName, preventedByLocking.get() ? "due to locking" : "");
            }
        });
    }

    @Test
    public void testPhantomWriteSelectColumn() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);

                List<Number> salaries = selectColumnList(aliceConnection, allEmployeeSalarySql(), Number.class);
                assertEquals(90_000, salaries.stream().mapToInt(Number::intValue).sum());

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                List<Number> _salaries = selectColumnList(bobConnection, allEmployeeSalarySql(), Number.class);
                                assertEquals(90_000, _salaries.stream().mapToInt(Number::intValue).sum());

                                try (
                                        PreparedStatement employeeStatement = bobConnection.prepareStatement(insertEmployeeSql());
                                ) {
                                    int employeeId = 4;
                                    int index = 0;
                                    employeeStatement.setLong(++index, 1);
                                    employeeStatement.setString(++index, "Carol");
                                    employeeStatement.setLong(++index, 9_000);
                                    employeeStatement.setLong(++index, employeeId);
                                    employeeStatement.executeUpdate();
                                }
                            } catch (Exception e) {
                                LOGGER.info("Exception thrown", e);
                                preventedByLocking.set(true);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.info("Exception thrown", e);
                    preventedByLocking.set(true);
                }
                update(aliceConnection, updateEmployeeSalarySql());
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            preventedByLocking.set(true);
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Read since the salary count is {} instead 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Read {}", isolationLevelName, preventedByLocking.get() ? "due to locking" : "");
            }
        });
    }

    @Test
    public void testPhantomWriteSelectColumnInOneTx() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);

                List<Number> salaries = selectColumnList(aliceConnection, allEmployeeSalarySql(), Number.class);
                assertEquals(90_000, salaries.stream().mapToInt(Number::intValue).sum());

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            try {
                                try (
                                        PreparedStatement employeeStatement = bobConnection.prepareStatement(insertEmployeeSql());
                                ) {
                                    int employeeId = 4;
                                    int index = 0;
                                    employeeStatement.setLong(++index, 1);
                                    employeeStatement.setString(++index, "Carol");
                                    employeeStatement.setLong(++index, 9_000);
                                    employeeStatement.setLong(++index, employeeId);
                                    employeeStatement.executeUpdate();
                                }
                            } catch (Exception e) {
                                LOGGER.info("Exception thrown", e);
                                preventedByLocking.set(true);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.info("Exception thrown", e);
                    preventedByLocking.set(true);
                }
                update(aliceConnection, updateEmployeeSalarySql());
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            preventedByLocking.set(true);
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Read since the salary count is {} instead 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Read {}", isolationLevelName, preventedByLocking.get() ? "due to locking" : "");
            }
        });
    }

    @Test
    public void testPhantomReadAggregateWithTableLock() {
        AtomicReference<Boolean> preventedByLocking = new AtomicReference<>();
        try {
            doInJDBC(aliceConnection -> {
                if (!aliceConnection.getMetaData().supportsTransactionIsolationLevel(isolationLevel)) {
                    LOGGER.info("Database {} doesn't support {}", dataSourceProvider().database(), isolationLevelName);
                    return;
                }
                prepareConnection(aliceConnection);
                executeStatement(aliceConnection, lockEmployeeTableSql());

                long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
                assertEquals(90_000, salaryCount);

                try {
                    executeSync(() -> {
                        doInJDBC(bobConnection -> {
                            prepareConnection(bobConnection);
                            executeStatement(bobConnection, lockEmployeeTableSql());
                            try {
                                long _salaryCount = selectColumn(bobConnection, sumEmployeeSalarySql(), Number.class).longValue();
                                assertEquals(90_000, _salaryCount);

                                try (
                                        PreparedStatement employeeStatement = bobConnection.prepareStatement(insertEmployeeSql());
                                ) {
                                    int employeeId = 4;
                                    int index = 0;
                                    employeeStatement.setLong(++index, 1);
                                    employeeStatement.setString(++index, "Carol");
                                    employeeStatement.setLong(++index, 9_000);
                                    employeeStatement.setLong(++index, employeeId);
                                    employeeStatement.executeUpdate();
                                }
                            } catch (Exception e) {
                                LOGGER.info("Exception thrown", e);
                                preventedByLocking.set(true);
                            }
                        });
                    });
                } catch (Exception e) {
                    LOGGER.info("Exception thrown", e);
                    preventedByLocking.set(true);
                }
                update(aliceConnection, updateEmployeeSalarySql());
            });
        } catch (Exception e) {
            LOGGER.info("Exception thrown", e);
            preventedByLocking.set(true);
        }
        doInJDBC(aliceConnection -> {
            long salaryCount = selectColumn(aliceConnection, sumEmployeeSalarySql(), Number.class).longValue();
            if(99_000 != salaryCount) {
                LOGGER.info("Isolation level {} allows Phantom Read even when using Explicit Locks since the salary count is {} instead 99000", isolationLevelName, salaryCount);
            }
            else {
                LOGGER.info("Isolation level {} prevents Phantom Read when using Explicit Locks {}", isolationLevelName, preventedByLocking.get() ? "due to locking" : "");
            }
        });
    }

    protected void prepareConnection(Connection connection) throws SQLException {
        connection.setTransactionIsolation(isolationLevel);
        try {
            connection.setNetworkTimeout(Executors.newSingleThreadExecutor(), 5000);
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
        return "SELECT COUNT(*) FROM post_comment where post_id = 1";
    }

    protected abstract String lockEmployeeTableSql();

    protected String countCommentsSqlForInitialVersion() {
        return "SELECT COUNT(*) FROM post_comment where post_id = 1 and version = 0";
    }

    protected String updateCommentsSql() {
        return "UPDATE post_comment SET version = 100 WHERE post_id = 1";
    }

    int nextId = 100;

    protected String insertCommentSql() {
        return String.format("INSERT INTO post_comment (post_id, review, version, id) VALUES (1, 'Phantom', 0, %d)", nextId++);
    }

    protected String sumEmployeeSalarySql() {
        return "SELECT SUM(salary) FROM employee where department_id = 1";
    }

    protected String allEmployeeSalarySql() {
        return "SELECT salary FROM employee where department_id = 1";
    }

    protected String insertEmployeeSql() {
        return INSERT_EMPLOYEE;
    }

    protected String updateEmployeeSalarySql() {
        return "UPDATE employee SET salary = salary * 1.1 WHERE department_id = 1";
    }

    @Entity(name = "Department")
    @Table(name = "department")
    public static class Department {

        @Id
        private Long id;

        private String name;

        private long budget;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getBudget() {
            return budget;
        }

        public void setBudget(long budget) {
            this.budget = budget;
        }
    }

    @Entity(name = "Employee")
    @Table(name = "employee", indexes = @Index(name = "IDX_Employee", columnList = "department_id"))
    public static class Employee {

        @Id
        private Long id;

        @Column(name = "name")
        private String name;

        @ManyToOne(fetch = FetchType.LAZY)
        private Department department;

        private long salary;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Department getDepartment() {
            return department;
        }

        public void setDepartment(Department department) {
            this.department = department;
        }

        public long getSalary() {
            return salary;
        }

        public void setSalary(long salary) {
            this.salary = salary;
        }
    }

}

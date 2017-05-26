package com.vladmihalcea.book.hpjp.jdbc.transaction.phenomena;

import com.microsoft.sqlserver.jdbc.SQLServerConnection;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.SQLServerDataSourceProvider;

import org.junit.runners.Parameterized;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * SQLServerPhenomenaTest - Test to validate SQL Server phenomena
 *
 * @author Vlad Mihalcea
 */
public class SQLServerPhenomenaTest extends AbstractPhenomenaTest {

    public SQLServerPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new SQLServerDataSourceProvider();
    }

    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        List<Object[]> levels = new ArrayList<>();
        levels.add(new Object[]{"Read Uncommitted", Connection.TRANSACTION_READ_UNCOMMITTED});
        levels.add(new Object[]{"Read Committed", Connection.TRANSACTION_READ_COMMITTED});
        levels.add(new Object[]{"Repeatable Read", Connection.TRANSACTION_REPEATABLE_READ});
        levels.add(new Object[]{"Serializable", Connection.TRANSACTION_SERIALIZABLE});
        levels.add(new Object[]{"Read Committed Snapshot Isolation", SQLServerConnection.TRANSACTION_READ_COMMITTED});
        levels.add(new Object[]{"Snapshot Isolation", SQLServerConnection.TRANSACTION_SNAPSHOT});
        return levels;
    }

    protected String selectPostTitleSql() {
        return "SELECT title FROM post WITH(NOWAIT) WHERE id = 1";
    }

    protected String updatePostTitleSql() {
        return "UPDATE post WITH(NOWAIT) SET title = 'ACID' WHERE id = 1";
    }

    protected String updatePostTitleParamSql() {
        return "UPDATE post WITH(NOWAIT) SET title = ? WHERE id = 1";
    }

    protected String updatePostDetailsAuthorParamSql() {
        return "UPDATE post_details WITH(NOWAIT) SET created_by = ? WHERE id = 1";
    }

    protected String insertCommentSql() {
        return "INSERT INTO post_comment WITH(NOWAIT) (post_id, review, version, id) VALUES (1, 'Phantom', 0, 1000)";
    }

    protected String insertEmployeeSql() {
        return "INSERT INTO employee WITH(NOWAIT) (department_id, name, salary, id) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected void prepareConnection(Connection connection) throws SQLException {
        try(Statement statement = connection.createStatement()) {
            String snapshot = getIsolationLevelName().contains("Snapshot") ? "ON" : "OFF";
            statement.executeUpdate("ALTER DATABASE high_performance_java_persistence SET READ_COMMITTED_SNAPSHOT " + snapshot);
            statement.executeUpdate("ALTER DATABASE high_performance_java_persistence SET ALLOW_SNAPSHOT_ISOLATION " + snapshot);
        }
        super.prepareConnection(connection);
    }
}

package com.vladmihalcea.book.hpjp.jdbc.transaction;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;
import org.junit.runners.Parameterized;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

/**
 * OracleConnectionReadyOnlyTransactionTest - Test to verify Oracle driver supports read-only transactions
 *
 * @author Vlad Mihalcea
 */
public class OracleConnectionReadyOnlyTransactionTest extends ConnectionReadyOnlyTransactionTest {

    public OracleConnectionReadyOnlyTransactionTest(Database database) {
        super(database);
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        return Collections.singletonList(new DataSourceProvider[]{new OracleDataSourceProvider()});
    }

    protected void setReadOnly(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try(CallableStatement statement = connection.prepareCall("begin set transaction read only; end;")) {
            statement.execute();
        }
    }
}

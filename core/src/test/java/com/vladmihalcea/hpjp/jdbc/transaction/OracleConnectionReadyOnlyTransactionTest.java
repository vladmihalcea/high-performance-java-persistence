package com.vladmihalcea.hpjp.jdbc.transaction;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.OracleDataSourceProvider;
import org.assertj.core.util.Arrays;
import org.junit.runners.Parameterized;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
    public static Collection<Database[]> databases() {
        List<Database[]> databases = new ArrayList<>();
        databases.add(Arrays.array(Database.ORACLE));
        return databases;
    }

    protected void setReadOnly(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try(CallableStatement statement = connection.prepareCall("begin set transaction read only; end;")) {
            statement.execute();
        }
    }
}

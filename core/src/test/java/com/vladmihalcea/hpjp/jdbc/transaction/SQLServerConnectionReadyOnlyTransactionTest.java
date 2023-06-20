package com.vladmihalcea.hpjp.jdbc.transaction;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Collections;

/**
 * SQLServerConnectionReadyOnlyTransactionTest - Test to verify SQL Server driver supports read-only transactions
 *
 * @author Vlad Mihalcea
 */
public class SQLServerConnectionReadyOnlyTransactionTest extends ConnectionReadyOnlyTransactionTest {

    public SQLServerConnectionReadyOnlyTransactionTest(Database database) {
        super(database);
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        return Collections.singletonList(new DataSourceProvider[]{
            new SQLServerDataSourceProvider() {
                @Override
                public DataSource dataSource() {
                    SQLServerDataSource dataSource = (SQLServerDataSource) super.dataSource();
                    dataSource.setURL(dataSource.getURL() + ";ApplicationIntent=ReadOnly");
                    return dataSource;
                }
            }}
        );
    }


}

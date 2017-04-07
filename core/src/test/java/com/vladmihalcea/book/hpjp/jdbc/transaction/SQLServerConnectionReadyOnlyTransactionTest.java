package com.vladmihalcea.book.hpjp.jdbc.transaction;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.SQLServerDataSourceProvider;

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

    public SQLServerConnectionReadyOnlyTransactionTest(DataSourceProvider dataSourceProvider) {
        super(dataSourceProvider);
    }

    @Parameterized.Parameters
    public static Collection<DataSourceProvider[]> rdbmsDataSourceProvider() {
        return Collections.singletonList(new DataSourceProvider[]{new SQLServerDataSourceProvider() {
            @Override
            public DataSource dataSource() {
                SQLServerDataSource dataSource = (SQLServerDataSource) super.dataSource();
                dataSource.setURL(dataSource.getURL() + ";ApplicationIntent=ReadOnly");
                return dataSource;
            }
        }});
    }


}

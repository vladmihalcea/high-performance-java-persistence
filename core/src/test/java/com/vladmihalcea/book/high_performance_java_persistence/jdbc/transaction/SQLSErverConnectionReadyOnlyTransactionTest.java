package com.vladmihalcea.book.high_performance_java_persistence.jdbc.transaction;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.vladmihalcea.book.high_performance_java_persistence.util.AbstractTest;
import com.vladmihalcea.book.high_performance_java_persistence.util.DataSourceProviderIntegrationTest;
import com.vladmihalcea.book.high_performance_java_persistence.util.providers.BatchEntityProvider;
import org.junit.Test;
import org.junit.runners.Parameterized;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

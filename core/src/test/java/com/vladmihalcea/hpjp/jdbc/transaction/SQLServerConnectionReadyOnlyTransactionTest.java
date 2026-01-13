package com.vladmihalcea.hpjp.jdbc.transaction;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import com.vladmihalcea.hpjp.util.providers.SQLServerDataSourceProvider;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.sql.DataSource;
import java.util.stream.Stream;

/**
 * SQLServerConnectionReadyOnlyTransactionTest - Test to verify SQL Server driver supports read-only transactions
 *
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
public class SQLServerConnectionReadyOnlyTransactionTest extends ConnectionReadyOnlyTransactionTest {

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(Database.SQLSERVER)
        );
    }

    protected DataSourceProvider dataSourceProvider() {
        return new SQLServerDataSourceProvider() {
            @Override
            public DataSource dataSource() {
                SQLServerDataSource dataSource = (SQLServerDataSource) super.dataSource();
                dataSource.setURL(dataSource.getURL() + ";ApplicationIntent=ReadOnly");
                return dataSource;
            }
        };
    }
}

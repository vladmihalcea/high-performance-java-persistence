package com.vladmihalcea.hpjp.util;

import com.vladmihalcea.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.hpjp.util.providers.Database;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
public abstract class DatabaseProviderIntegrationTest extends AbstractTest {

    @Parameter
    protected DataSourceProvider databaseSourceProvider;

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(Database.ORACLE.dataSourceProvider()),
            Arguments.of(Database.SQLSERVER.dataSourceProvider()),
            Arguments.of(Database.POSTGRESQL.dataSourceProvider()),
            Arguments.of(Database.MYSQL.dataSourceProvider())
        );
    }

    @Override
    protected Database database() {
        return databaseSourceProvider.database();
    }
}

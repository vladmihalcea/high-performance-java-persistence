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
public abstract class DatabaseIntegrationTest extends AbstractTest {

    @Parameter
    protected Database database;

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(Database.ORACLE),
            Arguments.of(Database.SQLSERVER),
            Arguments.of(Database.POSTGRESQL),
            Arguments.of(Database.MYSQL)
        );
    }

    @Override
    protected Database database() {
        return database;
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return database.dataSourceProvider();
    }
}

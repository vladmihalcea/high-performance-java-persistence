package com.vladmihalcea.hpjp.jdbc.transaction;

import com.vladmihalcea.hpjp.util.providers.Database;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Stream;

/**
 * OracleConnectionReadyOnlyTransactionTest - Test to verify Oracle driver supports read-only transactions
 *
 * @author Vlad Mihalcea
 */
@ParameterizedClass
@MethodSource("parameters")
public class OracleConnectionReadyOnlyTransactionTest extends ConnectionReadyOnlyTransactionTest {

    public static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(Database.ORACLE)
        );
    }

    protected void setReadOnly(Connection connection) throws SQLException {
        connection.setAutoCommit(false);
        try(CallableStatement statement = connection.prepareCall("begin set transaction read only; end;")) {
            statement.execute();
        }
    }
}

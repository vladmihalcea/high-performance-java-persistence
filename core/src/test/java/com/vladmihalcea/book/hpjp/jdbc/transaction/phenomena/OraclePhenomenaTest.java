package com.vladmihalcea.book.hpjp.jdbc.transaction.phenomena;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.runners.Parameterized;

import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.OracleDataSourceProvider;

/**
 * OraclePhenomenaTest - Test to validate Oracle phenomena
 *
 * @author Vlad Mihalcea
 */
public class OraclePhenomenaTest extends AbstractPhenomenaTest {

    public OraclePhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        List<Object[]> levels = new ArrayList<>();
        levels.add(new Object[]{"Read Committed", Connection.TRANSACTION_READ_COMMITTED});
        levels.add(new Object[]{"Serializable", Connection.TRANSACTION_SERIALIZABLE});
        return levels;
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new OracleDataSourceProvider();
    }
}

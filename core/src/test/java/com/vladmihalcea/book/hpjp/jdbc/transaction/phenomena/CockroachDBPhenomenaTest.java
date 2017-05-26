package com.vladmihalcea.book.hpjp.jdbc.transaction.phenomena;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.runners.Parameterized;

import com.vladmihalcea.book.hpjp.jdbc.transaction.phenomena.AbstractPhenomenaTest;
import com.vladmihalcea.book.hpjp.util.providers.CockroachDBDataSourceProvider;
import com.vladmihalcea.book.hpjp.util.providers.DataSourceProvider;

/**
 * @author Vlad Mihalcea
 */
public class CockroachDBPhenomenaTest extends AbstractPhenomenaTest {

    public CockroachDBPhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> isolationLevels() {
        List<Object[]> levels = new ArrayList<>();
        levels.add(new Object[]{"Serializable", Connection.TRANSACTION_SERIALIZABLE});
        return levels;
    }

    @Override
    protected DataSourceProvider dataSourceProvider() {
        return new CockroachDBDataSourceProvider();
    }
}

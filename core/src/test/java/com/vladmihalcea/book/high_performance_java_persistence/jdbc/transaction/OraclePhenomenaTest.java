package com.vladmihalcea.book.high_performance_java_persistence.jdbc.transaction;

/**
 * OraclePhenomenaTest - Test to validate Oracle phenomena
 *
 * @author Vlad Mihalcea
 */
public class OraclePhenomenaTest extends AbstractPhenomenaTest {

    public OraclePhenomenaTest(String isolationLevelName, int isolationLevel) {
        super(isolationLevelName, isolationLevel);
    }

    @Override
    protected DataSourceProvider getDataSourceProvider() {
        return new OracleDataSourceProvider();
    }
}

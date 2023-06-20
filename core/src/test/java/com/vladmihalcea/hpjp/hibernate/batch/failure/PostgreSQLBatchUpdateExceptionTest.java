package com.vladmihalcea.hpjp.hibernate.batch.failure;

import com.vladmihalcea.hpjp.util.providers.Database;

import java.sql.BatchUpdateException;

import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class PostgreSQLBatchUpdateExceptionTest extends AbstractBatchUpdateExceptionTest {

    @Override
    protected Database database() {
        return Database.POSTGRESQL;
    }

    @Override
    protected void onBatchUpdateException(BatchUpdateException e) {
        LOGGER.info("Batch failure", e);
    }
}

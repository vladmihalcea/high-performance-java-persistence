package com.vladmihalcea.book.hpjp.hibernate.batch.failure;

import com.vladmihalcea.book.hpjp.util.providers.Database;

import java.sql.BatchUpdateException;
import java.util.Arrays;

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

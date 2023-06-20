package com.vladmihalcea.hpjp.hibernate.batch.failure;

import com.vladmihalcea.hpjp.util.providers.Database;

import java.sql.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Vlad Mihalcea
 */
public class OracleBatchUpdateExceptionTest extends AbstractBatchUpdateExceptionTest {

    @Override
    protected Database database() {
        return Database.ORACLE;
    }

    @Override
    protected void onBatchUpdateException(BatchUpdateException e) {
        assertSame(2, e.getUpdateCounts().length);
        LOGGER.info(e.getMessage());
        LOGGER.info("Batch has managed to process {} entries", e.getUpdateCounts().length);
    }
}

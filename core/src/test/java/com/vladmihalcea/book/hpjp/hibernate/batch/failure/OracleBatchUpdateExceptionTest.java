package com.vladmihalcea.book.hpjp.hibernate.batch.failure;

import com.vladmihalcea.book.hpjp.util.AbstractTest;
import com.vladmihalcea.book.hpjp.util.providers.Database;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.junit.Test;

import jakarta.persistence.*;
import java.sql.*;
import java.util.*;

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

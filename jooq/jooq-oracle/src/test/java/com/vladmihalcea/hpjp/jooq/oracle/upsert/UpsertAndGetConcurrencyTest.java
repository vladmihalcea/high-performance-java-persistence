package com.vladmihalcea.hpjp.jooq.oracle.upsert;

import com.vladmihalcea.hpjp.jooq.oracle.schema.crud.tables.records.PostDetailsRecord;
import com.vladmihalcea.hpjp.jooq.oracle.util.AbstractJOOQOracleSQLIntegrationTest;
import com.vladmihalcea.hpjp.util.exception.ExceptionUtil;
import org.junit.Test;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.vladmihalcea.hpjp.jooq.oracle.schema.crud.Tables.POST;
import static com.vladmihalcea.hpjp.jooq.oracle.schema.crud.tables.PostDetails.POST_DETAILS;
import static org.jooq.impl.DSL.val;
import static org.junit.Assert.assertTrue;
import static org.jooq.impl.DSL.field;

/**
 * @author Vlad Mihalcea
 */
public class UpsertAndGetConcurrencyTest extends AbstractJOOQOracleSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "clean_schema.sql";
    }

    private final CountDownLatch aliceLatch = new CountDownLatch(1);

    @Test
    public void testUpsert() {
        doInJOOQ(sql -> {
            sql.delete(POST_DETAILS).execute();
            sql.delete(POST).execute();

            sql
            .insertInto(POST).columns(POST.ID, POST.TITLE)
            .values(1L, "High-Performance Java Persistence")
            .execute();

            sql
            .insertInto(POST_DETAILS)
            .columns(POST_DETAILS.ID, POST_DETAILS.CREATED_BY, POST_DETAILS.CREATED_ON)
            .values(1L, "Alice", LocalDateTime.now())
            .onDuplicateKeyIgnore()
            .execute();

            final AtomicBoolean preventedByLocking = new AtomicBoolean();

            executeAsync(() -> {
                try {
                    doInJOOQ(_sql -> {
                        setJdbcTimeout(_sql.configuration().connectionProvider().acquire());

                        _sql
                        .insertInto(POST_DETAILS)
                        .columns(POST_DETAILS.ID, POST_DETAILS.CREATED_BY, POST_DETAILS.CREATED_ON)
                        .values(1L, "Bob", LocalDateTime.now())
                        .onDuplicateKeyIgnore()
                        .execute();
                    });
                } catch (Exception e) {
                    if( ExceptionUtil.isLockTimeout( e )) {
                        preventedByLocking.set( true );
                    }
                }

                aliceLatch.countDown();
            });

            awaitOnLatch(aliceLatch);

            PostDetailsRecord postDetailsRecord = sql.selectFrom(POST_DETAILS)
                .where(field(POST_DETAILS.ID).eq(1L))
                .fetchOne();

            assertTrue(preventedByLocking.get());
        });
    }
}

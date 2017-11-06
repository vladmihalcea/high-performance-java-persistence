package com.vladmihalcea.book.hpjp.jooq.oracle.crud;

import com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.tables.records.PostDetailsRecord;
import com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.tables.records.PostRecord;
import com.vladmihalcea.book.hpjp.util.exception.ExceptionUtil;
import org.junit.Test;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.Sequences.HIBERNATE_SEQUENCE;
import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.Tables.POST;
import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.tables.PostDetails.POST_DETAILS;
import static org.jooq.impl.DSL.val;
import static org.junit.Assert.assertTrue;
import static org.jooq.impl.DSL.field;

/**
 * @author Vlad Mihalcea
 */
public class UpsertAndGetConcurrencyTest extends AbstractJOOQOracleSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    private final CountDownLatch aliceLatch = new CountDownLatch(1);

    @Test
    public void testUpsert() {
        doInJOOQ(sql -> {
            sql.delete(POST_DETAILS).execute();
            sql.delete(POST).execute();

            PostRecord postRecord = sql
            .insertInto(POST).columns(POST.ID, POST.TITLE)
            .values(HIBERNATE_SEQUENCE.nextval(), val("High-Performance Java Persistence"))
            .returning(POST.ID)
            .fetchOne();

            final BigInteger postId = postRecord.getId();

            sql
            .insertInto(POST_DETAILS)
            .columns(POST_DETAILS.ID, POST_DETAILS.CREATED_BY, POST_DETAILS.CREATED_ON)
            .values(postId, "Alice", Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
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
                        .values(postId, "Bob", Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
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
                .where(field(POST_DETAILS.ID).eq(postId))
                .fetchOne();

            assertTrue(preventedByLocking.get());
        });
    }
}

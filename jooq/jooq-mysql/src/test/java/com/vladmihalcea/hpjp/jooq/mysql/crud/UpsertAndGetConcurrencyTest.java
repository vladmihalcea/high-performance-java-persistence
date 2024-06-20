package com.vladmihalcea.hpjp.jooq.mysql.crud;

import com.vladmihalcea.hpjp.jooq.mysql.schema.crud.tables.records.PostDetailsRecord;
import com.vladmihalcea.hpjp.jooq.mysql.schema.crud.tables.records.PostRecord;
import com.vladmihalcea.util.exception.ExceptionUtil;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.vladmihalcea.hpjp.jooq.mysql.schema.crud.Tables.POST;
import static com.vladmihalcea.hpjp.jooq.mysql.schema.crud.tables.PostDetails.POST_DETAILS;
import static org.jooq.impl.DSL.val;
import static org.jooq.impl.DSL.field;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class UpsertAndGetConcurrencyTest extends AbstractJOOQMySQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "clean_schema.sql";
    }

    private final CountDownLatch aliceLatch = new CountDownLatch(1);

    @Test(timeout = 3000)
    public void testUpsert() {
        doInJOOQ(sql -> {
            sql.delete(POST_DETAILS).execute();
            sql.delete(POST).execute();

            PostRecord postRecord = sql
            .insertInto(POST).columns(POST.TITLE)
            .values(val("High-Performance Java Persistence"))
            .returning(POST.ID)
            .fetchOne();

            final Long postId = postRecord.getId();

            sql
            .insertInto(POST_DETAILS)
            .columns(POST_DETAILS.ID, POST_DETAILS.CREATED_BY, POST_DETAILS.CREATED_ON)
            .values(postId, "Alice", LocalDateTime.now())
            .onDuplicateKeyIgnore()
            .execute();

            final AtomicBoolean preventedByLocking = new AtomicBoolean();

            executeAsync(() -> {
                try {
                    doInJOOQ(_sql -> {
                        Connection connection = _sql.configuration().connectionProvider().acquire();
                        setJdbcTimeout(connection);

                        Thread shutdownThread = new Thread(() -> {
                            sleep(1500);
                            try {
                                if (!connection.isClosed()) {
                                    connection.close();
                                }
                                preventedByLocking.set( true );
                                aliceLatch.countDown();
                            } catch (SQLException ignore) {}
                        });
                        shutdownThread.setDaemon(true);
                        shutdownThread.start();

                        _sql
                        .insertInto(POST_DETAILS)
                        .columns(POST_DETAILS.ID, POST_DETAILS.CREATED_BY, POST_DETAILS.CREATED_ON)
                        .values(postId, "Bob", LocalDateTime.now())
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

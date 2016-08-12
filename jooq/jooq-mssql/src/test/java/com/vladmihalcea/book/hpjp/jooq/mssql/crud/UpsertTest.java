package com.vladmihalcea.book.hpjp.jooq.mssql.crud;

import org.jooq.DSLContext;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static com.vladmihalcea.book.hpjp.jooq.mssql.schema.crud.high_performance_java_persistence.dbo.Tables.POST;
import static com.vladmihalcea.book.hpjp.jooq.mssql.schema.crud.high_performance_java_persistence.dbo.Tables.POST_DETAILS;

/**
 * @author Vlad Mihalcea
 */
public class UpsertTest extends AbstractJOOQSQLServerSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Test
    public void testUpsert() {
        doInJOOQ(sql -> {
            sql.delete(POST_DETAILS).execute();
            sql.delete(POST).execute();
            sql
            .insertInto(POST).columns(POST.ID, POST.TITLE)
            .values(1L, "High-Performance Java Persistence")
            .execute();

            executeAsync(() -> {
                upsertPostDetails(sql, 1L, "Alice",
                        Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
            });
            executeAsync(() -> {
                upsertPostDetails(sql, 1L, "Bob",
                        Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
            });

            awaitTermination(1, TimeUnit.SECONDS);
        });
    }

    private void upsertPostDetails(DSLContext sql, Long id, String owner, Timestamp timestamp) {
        sql
        .insertInto(POST_DETAILS)
        .columns(POST_DETAILS.ID, POST_DETAILS.CREATED_BY, POST_DETAILS.CREATED_ON)
        .values(id, owner, timestamp)
        .onDuplicateKeyUpdate()
        .set(POST_DETAILS.UPDATED_BY, owner)
        .set(POST_DETAILS.UPDATED_ON, timestamp)
        .execute();
    }
}

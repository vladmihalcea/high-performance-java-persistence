package com.vladmihalcea.book.hpjp.jooq.pgsql.upsert;

import com.vladmihalcea.book.hpjp.jooq.pgsql.util.AbstractJOOQPostgreSQLIntegrationTest;
import org.jooq.DSLContext;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.POST;
import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.PostDetails.POST_DETAILS;

/**
 * @author Vlad Mihalcea
 */
public class UpsertTest extends AbstractJOOQPostgreSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "clean_schema.sql";
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
                upsertPostDetails(sql, 1L, "Alice", LocalDateTime.now());
            });
            executeAsync(() -> {
                upsertPostDetails(sql, 1L, "Bob", LocalDateTime.now());
            });

            awaitTermination(1, TimeUnit.SECONDS);
        });
    }

    private void upsertPostDetails(DSLContext sql, Long id, String owner, LocalDateTime timestamp) {
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

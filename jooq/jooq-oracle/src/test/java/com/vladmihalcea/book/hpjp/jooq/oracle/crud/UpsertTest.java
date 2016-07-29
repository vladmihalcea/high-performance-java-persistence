package com.vladmihalcea.book.hpjp.jooq.oracle.crud;

import org.jooq.DSLContext;
import org.junit.Test;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.Tables.POST;
import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.tables.PostDetails.POST_DETAILS;

/**
 * @author Vlad Mihalcea
 */
public class UpsertTest extends AbstractJOOQOracleSQLIntegrationTest {

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
            .values(BigInteger.valueOf(1), "High-Performance Java Persistence")
            .execute();

            upsert(sql, BigInteger.valueOf(1), "Vlad Mihalcea", Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
            upsert(sql, BigInteger.valueOf(1), "Vlad Mihalcea", Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        });
    }

    private void upsert(DSLContext sql, BigInteger id, String owner, Timestamp timestamp) {
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

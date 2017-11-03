package com.vladmihalcea.book.hpjp.jooq.pgsql.crud;

import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.records.PostDetailsRecord;
import org.jooq.DSLContext;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.Tables.POST;
import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.PostDetails.POST_DETAILS;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

/**
 * @author Vlad Mihalcea
 */
public class UpsertAndGetTest extends AbstractJOOQPostgreSQLIntegrationTest {

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

            PostDetailsRecord postDetailsRecord = upsertPostDetails(sql, 1L, "Alice",
                    Timestamp.from(LocalDateTime.now().toInstant(ZoneOffset.UTC)));
        });
    }

    private PostDetailsRecord upsertPostDetails(DSLContext sql, Long id, String owner, Timestamp timestamp) {
        sql
        .insertInto(POST_DETAILS)
        .columns(POST_DETAILS.ID, POST_DETAILS.CREATED_BY, POST_DETAILS.CREATED_ON)
        .values(id, owner, timestamp)
        .onDuplicateKeyIgnore()
        .execute();

        return sql.selectFrom(POST_DETAILS)
        .where(field(POST_DETAILS.ID).eq(id))
        .fetchOne();
    }
}

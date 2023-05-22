package com.vladmihalcea.book.hpjp.jooq.pgsql.upsert;

import com.vladmihalcea.book.hpjp.jooq.pgsql.schema.crud.tables.records.PostDetailsRecord;
import com.vladmihalcea.book.hpjp.jooq.pgsql.util.AbstractJOOQPostgreSQLIntegrationTest;
import org.jooq.DSLContext;
import org.junit.Test;

import java.time.LocalDateTime;

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

            PostDetailsRecord postDetailsRecord = upsertPostDetails(sql, 1L, "Alice", LocalDateTime.now());
        });
    }

    private PostDetailsRecord upsertPostDetails(DSLContext sql, Long id, String owner, LocalDateTime timestamp) {
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

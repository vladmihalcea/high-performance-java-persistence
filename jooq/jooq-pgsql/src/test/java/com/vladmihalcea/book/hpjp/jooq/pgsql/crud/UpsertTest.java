package com.vladmihalcea.book.hpjp.jooq.pgsql.crud;

import org.jooq.DSLContext;
import org.junit.Test;

import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.Tables.POST;

/**
 * @author Vlad Mihalcea
 */
public class UpsertTest extends AbstractJOOQPostgreSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Test
    public void testUpsert() {
        doInJOOQ(context -> {
            context.delete(POST).execute();
            upsert(context);
            upsert(context);
        });
    }

   private void upsert(DSLContext context) {
       context
           .insertInto(POST)
           .columns(POST.ID, POST.TITLE)
           .values(1L, "High-Performance Java Persistence")
           .onDuplicateKeyUpdate()
           .set(POST.TITLE, "High-Performance Java Persistence 2nd Edition")
           .execute();
   }
}

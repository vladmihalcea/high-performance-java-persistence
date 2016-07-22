package com.vladmihalcea.book.hpjp.jooq.mysql.crud;

import org.jooq.DSLContext;
import org.junit.Test;

import static com.vladmihalcea.book.hpjp.jooq.mysql.schema.Tables.POST;

/**
 * @author Vlad Mihalcea
 */
public class UpsertTest extends AbstractJOOQMySQLIntegrationTest {

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
           .columns(POST.TITLE)
           .values("High-Performance Java Persistence")
           .onDuplicateKeyUpdate()
           .set(POST.TITLE, "High-Performance Java Persistence 2nd Edition")
           .execute();
   }
}

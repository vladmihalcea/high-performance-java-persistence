package com.vladmihalcea.book.hpjp.jooq.mssql.crud;

import org.jooq.DSLContext;
import org.junit.Test;

import static com.vladmihalcea.book.hpjp.jooq.mssql.schema.high_performance_java_persistence.dbo.Tables.POST;

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
            sql.delete(POST).execute();
            upsert(sql);
            upsert(sql);
        });
    }

   private void upsert(DSLContext sql) {
       sql
           .insertInto(POST)
           .columns(POST.ID, POST.TITLE)
           .values(1L, "High-Performance Java Persistence")
           .onDuplicateKeyUpdate()
           .set(POST.TITLE, "High-Performance Java Persistence Book")
           .execute();
   }
}

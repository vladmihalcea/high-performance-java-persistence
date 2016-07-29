package com.vladmihalcea.book.hpjp.jooq.oracle.crud;

import org.jooq.DSLContext;
import org.junit.Test;

import java.math.BigInteger;

import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.Tables.POST;

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
            sql.delete(POST).execute();
            upsert(sql);
            upsert(sql);
        });
    }

   private void upsert(DSLContext sql) {
       sql
           .insertInto(POST)
           .columns(POST.ID, POST.TITLE)
           .values(BigInteger.ONE, "High-Performance Java Persistence")
           .onDuplicateKeyUpdate()
           .set(POST.TITLE, "High-Performance Java Persistence Book")
           .execute();
   }
}

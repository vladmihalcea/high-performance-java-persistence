package com.vladmihalcea.book.hpjp.jooq.oracle.crud;

import org.junit.Test;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class CrudTest extends AbstractJOOQOracleSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Test
    public void testCrud() {
        doInJOOQ(sql -> {
            sql
            .deleteFrom(table("post"))
            .execute();

            assertEquals(1, sql
            .insertInto(table("post")).columns(field("id"), field("title"))
            .values(1, "High-Performance Java Persistence")
            .execute());

            assertEquals("High-Performance Java Persistence", sql
            .select(field("title"))
            .from(table("post"))
            .where(field("id").eq(1))
            .fetch().getValue(0, "title"));

            sql
            .update(table("post"))
            .set(field("title"), "High-Performance Java Persistence Book")
            .where(field("id").eq(1))
            .execute();

            assertEquals("High-Performance Java Persistence Book", sql
                    .select(field("title"))
                    .from(table("post"))
                    .where(field("id").eq(1))
                    .fetch().getValue(0, "title"));
        });
    }
}

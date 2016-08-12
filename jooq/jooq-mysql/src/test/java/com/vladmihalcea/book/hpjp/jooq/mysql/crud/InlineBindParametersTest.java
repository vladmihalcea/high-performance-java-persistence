package com.vladmihalcea.book.hpjp.jooq.mysql.crud;

import org.jooq.DSLContext;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.junit.Test;

import java.util.List;

import static com.vladmihalcea.book.hpjp.jooq.mysql.schema.crud.Tables.POST;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class InlineBindParametersTest extends AbstractJOOQMySQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Test
    public void test() {
        doInJOOQ(sql -> {
            sql
            .deleteFrom(table("post"))
            .execute();

            assertEquals(1, sql
            .insertInto(table("post")).columns(field("id"), field("title"))
            .values(1L, "High-Performance Java Persistence")
            .execute());

            List<String> titles = sql
                .select(POST.TITLE)
                .from(POST)
                .where(POST.ID.eq(1L))
                .fetch(POST.TITLE);
            assertEquals(1, titles.size());
        });

        doInJDBC(connection -> {
            DSLContext sql = DSL.using(
                connection,
                sqlDialect(),
                new Settings().withStatementType(StatementType.STATIC_STATEMENT)
            );

            List<String> titles = sql
            .select(POST.TITLE)
            .from(POST)
            .where(POST.ID.eq(1L))
            .fetch(POST.TITLE);

            assertEquals(1, titles.size());
        });

    }
}

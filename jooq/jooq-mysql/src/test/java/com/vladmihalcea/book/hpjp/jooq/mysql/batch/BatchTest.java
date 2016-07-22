package com.vladmihalcea.book.hpjp.jooq.mysql.batch;

import org.jooq.Record;
import org.jooq.Result;
import org.junit.Test;

import static com.vladmihalcea.book.hpjp.jooq.mysql.schema.Tables.POST;


/**
 * @author Vlad Mihalcea
 */
public class BatchTest extends AbstractJOOQMySQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Test
    public void testBatching() {
        doInJOOQ(context -> {
            try {
                Result<Record> result = context.select().from(POST).fetch();
            } catch (org.jooq.exception.DataAccessException e) {
                e.printStackTrace();
            }
        });
    }
}

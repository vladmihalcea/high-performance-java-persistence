package com.vladmihalcea.book.hpjp.jooq.mssql.crud;

import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Test;

import java.math.BigInteger;

import static com.vladmihalcea.book.hpjp.jooq.mssql.schema.crud.high_performance_java_persistence.dbo.Tables.POST;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BatchTest extends AbstractJOOQSQLServerSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Test
    public void testBatching() {
        doInJOOQ(sql -> {
            sql.delete(POST).execute();
            BatchBindStep batch = sql.batch(sql
                .insertInto(POST, POST.ID, POST.TITLE)
                .values((Long) null, null)
            );
            for (int i = 0; i < 3; i++) {
                batch.bind(i, String.format("Post no. %d", i));
            }
            int[] insertCounts = batch.execute();
            assertEquals(3, insertCounts.length);
            Result<Record> posts = sql.select().from(POST).fetch();
            assertEquals(3, posts.size());
        });
    }
}

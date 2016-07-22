package com.vladmihalcea.book.hpjp.jooq.pgsql.batch;

import org.jooq.BatchBindStep;
import org.jooq.Record;
import org.jooq.Result;
import org.junit.Test;

import static com.vladmihalcea.book.hpjp.jooq.pgsql.schema.Tables.POST;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class BatchTest extends AbstractJOOQPostgreSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Test
    public void testBatching() {
        doInJOOQ(context -> {
            context.delete(POST).execute();
            BatchBindStep batch = context.batch(context
                .insertInto(POST, POST.ID, POST.TITLE)
                .values((Long) null, null)
            );
            for (int i = 0; i < 3; i++) {
                batch.bind(i, String.format("Post no. %d", i));
            }
            int[] insertCounts = batch.execute();
            assertEquals(3, insertCounts.length);
            Result<Record> posts = context.select().from(POST).fetch();
            assertEquals(3, posts.size());
        });
    }
}

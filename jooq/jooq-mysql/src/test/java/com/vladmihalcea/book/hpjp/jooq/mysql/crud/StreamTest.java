package com.vladmihalcea.book.hpjp.jooq.mysql.crud;

import org.junit.Before;
import org.junit.Test;

import static com.vladmihalcea.book.hpjp.jooq.mysql.schema.crud.Tables.POST;
import static com.vladmihalcea.book.hpjp.jooq.mysql.schema.crud.Tables.POST_COMMENT_DETAILS;

/**
 * @author Vlad Mihalcea
 */
public class StreamTest extends AbstractJOOQMySQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Before
    public void init() {
        super.init();

        doInJOOQ(sql -> {
            sql
            .deleteFrom(POST)
            .execute();

            long id = 0L;

            sql
            .insertInto(
                POST_COMMENT_DETAILS).columns(
                POST_COMMENT_DETAILS.ID,
                POST_COMMENT_DETAILS.POST_ID,
                POST_COMMENT_DETAILS.USER_ID,
                POST_COMMENT_DETAILS.IP,
                POST_COMMENT_DETAILS.FINGERPRINT
            )
            .values(++id, 1L, 1L, "192.168.0.2", "ABC123")
            .values(++id, 1L, 2L, "192.168.0.3", "ABC456")
            .values(++id, 1L, 3L, "192.168.0.4", "ABC789")
            .values(++id, 2L, 1L, "192.168.0.2", "ABC123")
            .values(++id, 2L, 2L, "192.168.0.3", "ABC456")
            .values(++id, 2L, 4L, "192.168.0.3", "ABC456")
            .values(++id, 2L, 5L, "192.168.0.3", "ABC456")
            .execute();
        });
    }

    @Test
    public void testStream() {
        doInJOOQ(sql -> {


        });
    }
}

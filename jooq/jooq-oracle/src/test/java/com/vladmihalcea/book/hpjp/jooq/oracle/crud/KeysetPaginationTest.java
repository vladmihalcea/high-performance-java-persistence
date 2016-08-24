package com.vladmihalcea.book.hpjp.jooq.oracle.crud;

import org.jooq.Record3;
import org.jooq.SelectSeekStep2;
import org.junit.Test;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.Tables.POST;
import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.Tables.POST_DETAILS;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class KeysetPaginationTest extends AbstractJOOQOracleSQLIntegrationTest {

    @Override
    protected String ddlScript() {
        return "initial_schema.sql";
    }

    @Test
    public void testPagination() {
        String user = "Vlad Mihalcea";

        doInJOOQ(sql -> {
            sql
            .deleteFrom(POST_DETAILS)
            .execute();

            sql
            .deleteFrom(POST)
            .execute();

            LocalDateTime now = LocalDateTime.now();

            for (long i = 1; i < 100; i++) {
                sql
                .insertInto(POST).columns(POST.ID, POST.TITLE)
                .values(BigInteger.valueOf(i), String.format("High-Performance Java Persistence - Chapter %d", i))
                .execute();

                sql
                .insertInto(POST_DETAILS).columns(POST_DETAILS.ID, POST_DETAILS.CREATED_ON, POST_DETAILS.CREATED_BY)
                .values(BigInteger.valueOf(i), Timestamp.valueOf(now.plusHours(i / 10)), user)
                .execute();
            }
        });

        doInJOOQ(sql -> {

            int pageSize = 5;

            List<PostSummary> results = nextPage(pageSize, null);

            assertEquals(5, results.size());

            results = nextPage(pageSize, results.get(results.size() - 1));

            assertEquals(5, results.size());
        });

        doInJOOQ(sql -> {

            int pageSize = 5;

            PostSummary offsetPostSummary = null;

            int pageCount = 0;

            while (true) {
                List<PostSummary> results = nextPage(pageSize, offsetPostSummary);
                if(results.isEmpty()) {
                    break;
                }

                offsetPostSummary = results.get(results.size() - 1);
                pageCount++;
            }

            assertEquals(Long.valueOf(1), offsetPostSummary.getId());
            assertEquals(20, pageCount);
        });
    }

    public List<PostSummary> nextPage(int pageSize, PostSummary offsetPostSummary) {
        return doInJOOQ(sql -> {
            SelectSeekStep2<Record3<BigInteger, String, Timestamp>, Timestamp, BigInteger> selectStep = sql
                    .select(POST.ID, POST.TITLE, POST_DETAILS.CREATED_ON)
                    .from(POST)
                    .join(POST_DETAILS).on(POST.ID.eq(POST_DETAILS.ID))
                    .orderBy(POST_DETAILS.CREATED_ON.desc(), POST.ID.desc());

            return (offsetPostSummary != null)
                    ? selectStep
                    .seek(offsetPostSummary.getCreatedOn(), BigInteger.valueOf(offsetPostSummary.getId()))
                    .limit(pageSize)
                    .fetchInto(PostSummary.class)
                    : selectStep
                    .limit(pageSize)
                    .fetchInto(PostSummary.class);
        });
    }

    /**
     * @author Vlad Mihalcea
     */
    public static class PostSummary {

        private final Long id;

        private final String title;

        private final Timestamp createdOn;

        public PostSummary(Long id, String title, Timestamp createdOn) {
            this.id = id;
            this.title = title;
            this.createdOn = createdOn;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Timestamp getCreatedOn() {
            return createdOn;
        }
    }
}

package com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset;

import com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.record.CommentRecord;
import com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.record.PostRecord;
import com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.record.TagRecord;
import com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.record.UserVoteRecord;
import org.jooq.Records;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import static com.vladmihalcea.hpjp.jooq.oracle.schema.crud.Tables.*;
import static org.jooq.impl.DSL.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MultiLevelCollectionFetchingMultisetTest extends AbstractMultiLevelCollectionFetchingTest {

    @Test
    public void testMultiset() {
        Long minPostId = 1L;
        Long maxPostId = 50L;

        doInJOOQ(sql -> {
            sql.execute("ALTER SESSION SET STATISTICS_LEVEL='ALL'");

            List<PostRecord> posts = sql
                .select(
                    POST.ID.as("id"),
                    POST.TITLE,
                    multiset(
                        select(
                            POST_COMMENT.ID,
                            POST_COMMENT.REVIEW,
                            multiset(
                                select(
                                    USER_VOTE.ID,
                                    concat(
                                        USER.FIRST_NAME,
                                        inline(" "),
                                        USER.LAST_NAME
                                    ),
                                    USER_VOTE.VOTE_TYPE
                                )
                                .from(USER_VOTE)
                                .innerJoin(USER).on(USER.ID.eq(USER_VOTE.USER_ID))
                                .where(USER_VOTE.COMMENT_ID.eq(POST_COMMENT.ID))
                            ).as("votes").convertFrom(
                                r -> r.map(Records.mapping(UserVoteRecord::new))
                            )
                        )
                        .from(POST_COMMENT)
                        .where(POST_COMMENT.POST_ID.eq(POST.ID))
                    ).as("comments").convertFrom(
                        r -> r.map(Records.mapping(CommentRecord::new))
                    ),
                    multiset(
                        select(
                            POST_TAG.tag().ID,
                            POST_TAG.tag().NAME
                        )
                        .from(POST_TAG)
                        .where(POST_TAG.POST_ID.eq(POST.ID))
                    ).as("tags").convertFrom(
                        r -> r.map(Records.mapping(TagRecord::new))
                    )
                )
                .from(POST)
                .where(POST.ID.between(minPostId, maxPostId))
                .orderBy(POST.ID.asc())
                .fetch(Records.mapping(PostRecord::new));

            assertEquals(POST_COUNT, posts.size());
            PostRecord post = posts.get(0);
            assertEquals(POST_COMMENT_COUNT, post.comments().size());
            assertEquals(TAG_COUNT, post.tags().size());
            CommentRecord comment = post.comments().get(0);
            assertEquals(VOTE_COUNT, comment.votes().size());

            String executionPlan = sql
                .select()
                .from("TABLE(DBMS_XPLAN.DISPLAY_CURSOR(FORMAT=>'ALLSTATS LAST ALL +OUTLINE'))")
                .stream()
                .map(record -> String.valueOf(record.get(0)))
                .collect(Collectors.joining(System.lineSeparator()));

            LOGGER.info("Execution plan: {}{}",
                System.lineSeparator(),
                executionPlan
            );

            sql.execute("ALTER SESSION SET STATISTICS_LEVEL='TYPICAL'");
        });

        doInJPA(entityManager -> {
            entityManager.createNativeQuery("""
                select /*+ GATHER_PLAN_STATISTICS */
                    cast("POST"."ID" as number(19)) "id",
                    "POST"."TITLE",
                    (
                        select
                            coalesce(
                                json_arrayagg(
                                    json_array(
                                        "v0",
                                        "v1",
                                        "v2"
                                        format json null on null returning clob
                                    ) format json returning clob
                                ),
                                json_array(returning clob)
                            )
                        from (
                            select
                                cast("POST_COMMENT"."ID" as number(19)) "v0",
                                 "POST_COMMENT"."REVIEW" "v1",
                                 (
                                    select
                                        coalesce(
                                            json_arrayagg(
                                                json_array(
                                                    "v0",
                                                    "v1",
                                                    "v2" null on null returning clob
                                                ) format json returning clob),
                                            json_array(returning clob)
                                        )
                                    from (
                                        select
                                            cast("USER_VOTE"."ID" as number(19)) "v0",
                                            (
                                                ("USER"."FIRST_NAME" || rpad(' ', 1, ' ')) ||
                                                "USER"."LAST_NAME"
                                            ) "v1",
                                            "USER_VOTE"."VOTE_TYPE" "v2"
                                        from
                                            "USER_VOTE"
                                        left outer join
                                            "USER" on "USER"."ID" = "USER_VOTE"."USER_ID"
                                        where
                                            "USER_VOTE"."COMMENT_ID" = "POST_COMMENT"."ID") "t"
                                ) "v2"
                                from
                                    "POST_COMMENT"
                                where
                                    "POST_COMMENT"."POST_ID" = "POST"."ID"
                        ) "t"
                    ) "comments",
                    (
                        select
                            coalesce(
                                json_arrayagg(
                                    json_array(
                                        "v0",
                                        "v1"
                                        null on null returning clob
                                    ) format json returning clob
                                ),
                                json_array(returning clob)
                            )
                        from (
                            select
                                cast("alias_111264759"."ID" as number(19)) "v0",
                                "alias_111264759"."NAME" "v1"
                            from (
                                "POST_TAG"
                                join
                                    "TAG" "alias_111264759"
                                        on "POST_TAG"."TAG_ID" = "alias_111264759"."ID"
                            )
                            where
                                "POST_TAG"."POST_ID" = "POST"."ID") "t"
                    ) "tags"
                from
                    "POST"
                where
                    "POST"."ID" between :minId and :maxId
                order by
                    "POST"."ID" asc
                """)
            .setParameter("minId", 1)
            .setParameter("maxId", 50)
            .getResultList();

            List<String> planLines = entityManager.createNativeQuery("""
                SELECT *
                FROM TABLE(DBMS_XPLAN.DISPLAY_CURSOR(FORMAT=>'ALLSTATS LAST ALL +OUTLINE'))
			    """)
            .getResultList();

            LOGGER.info("Execution plan: {}{}",
                System.lineSeparator(),
                planLines.stream().collect(Collectors.joining(System.lineSeparator()))
            );
        });
    }
}

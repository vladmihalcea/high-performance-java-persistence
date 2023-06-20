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
        BigInteger minPostId = BigInteger.valueOf(1);
        BigInteger maxPostId = BigInteger.valueOf(50);

        doInJOOQ(sql -> {
            sql.execute("ALTER SESSION SET STATISTICS_LEVEL='ALL'");

            List<PostRecord> posts = sql
                .select(
                    POST.ID.cast(Long.class).as("id"),
                    POST.TITLE,
                    multiset(
                        select(
                            POST_COMMENT.ID.cast(Long.class),
                            POST_COMMENT.REVIEW,
                            multiset(
                                select(
                                    USER_VOTE.ID.cast(Long.class),
                                    concat(
                                        USER.FIRST_NAME,
                                        space(1),
                                        USER.LAST_NAME
                                    ),
                                    USER_VOTE.VOTE_TYPE
                                )
                                .from(USER_VOTE)
                                .leftOuterJoin(USER).on(USER.ID.eq(USER_VOTE.USER_ID))
                                .where(USER_VOTE.COMMENT_ID.eq(POST_COMMENT.ID))
                            ).as("votes").convertFrom(r -> r.map(Records.mapping(UserVoteRecord::new)))
                        )
                        .from(POST_COMMENT)
                        .where(POST_COMMENT.POST_ID.eq(POST.ID))
                    ).as("comments").convertFrom(r -> r.map(Records.mapping(CommentRecord::new))),
                    multiset(
                        select(
                            POST_TAG.tag().ID.cast(Long.class),
                            POST_TAG.tag().NAME
                        )
                        .from(POST_TAG)
                        .where(POST_TAG.POST_ID.eq(POST.ID))
                    ).as("tags").convertFrom(r -> r.map(Records.mapping(TagRecord::new)))
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
    }
}

package com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset;

import com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset.record.CommentRecord;
import com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset.record.PostRecord;
import com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset.record.TagRecord;
import com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset.record.UserVoteRecord;
import org.jooq.Records;
import org.junit.Test;

import java.util.List;

import static com.vladmihalcea.book.hpjp.jooq.oracle.schema.crud.Tables.*;
import static org.jooq.impl.DSL.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MultiLevelCollectionFetchingMultisetTest extends AbstractMultiLevelCollectionFetchingTest {

    @Test
    public void testMultiset() {
        doInJOOQ(sql -> {
            List<PostRecord> posts = sql
                .select(
                    POST.ID.cast(Long.class),
                    POST.TITLE,
                    multiset(
                        select(
                            POST_COMMENT.ID.cast(Long.class),
                            POST_COMMENT.REVIEW,
                            multiset(
                                select(
                                    USER_VOTE.ID.cast(Long.class),
                                    concat(
                                        BLOG_USER.FIRST_NAME,
                                        space(1),
                                        BLOG_USER.LAST_NAME
                                    ),
                                    USER_VOTE.VOTE_TYPE
                                )
                                .from(USER_VOTE)
                                .leftOuterJoin(BLOG_USER).on(BLOG_USER.ID.eq(USER_VOTE.USER_ID))
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
                .orderBy(POST.ID.asc())
                .fetch(Records.mapping(PostRecord::new));

            assertEquals(POST_COUNT, posts.size());
            PostRecord post = posts.get(0);
            assertEquals(POST_COMMENT_COUNT, post.comments().size());
            assertEquals(TAG_COUNT, post.tags().size());
            CommentRecord comment = post.comments().get(0);
            assertEquals(VOTE_COUNT, comment.votes().size());
        });
    }
}

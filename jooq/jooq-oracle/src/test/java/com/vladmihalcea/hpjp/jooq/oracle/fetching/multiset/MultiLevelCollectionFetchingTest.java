package com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset;

import com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.domain.Post;
import com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.domain.PostComment;
import com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.record.*;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.vladmihalcea.hpjp.jooq.oracle.schema.crud.Tables.*;
import static org.jooq.impl.DSL.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class MultiLevelCollectionFetchingTest extends AbstractMultiLevelCollectionFetchingTest {

    @Test
    public void testTwoJoinFetchQueries() {
        List<Post> posts = doInJPA(entityManager -> {
            List<Post> _posts = entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.comments
                where p.id between :minId and :maxId
                """, Post.class)
            .setParameter("minId", 1L)
            .setParameter("maxId", 50L)
            .getResultList();

            entityManager.createQuery("""
                select p
                from Post p
                left join fetch p.tags t
                where p in :posts
                """, Post.class)
            .setParameter("posts", _posts)
            .getResultList();

            entityManager.createQuery("""
                select pc
                from PostComment pc
                left join fetch pc.votes v
                join pc.post p
                where p in :posts
                """, PostComment.class)
            .setParameter("posts", _posts)
            .getResultList();

            return _posts;
        });

        assertEquals(POST_COUNT, posts.size());

        for (Post post : posts) {
            assertEquals(POST_COMMENT_COUNT, post.getComments().size());
            for(PostComment comment : post.getComments()) {
                assertEquals(VOTE_COUNT, comment.getVotes().size());
            }
            assertEquals(TAG_COUNT, post.getTags().size());
        }
    }

    @Test
    public void testCartesianProduct() {
        Long minPostId = 1L;
        Long maxPostId = 50L;

        doInJOOQ(sql -> {
            sql.execute("ALTER SESSION SET STATISTICS_LEVEL='ALL'");

            List<FlatPostRecord> posts = sql
                .select(
                    POST.ID,
                    POST.TITLE,
                    POST_COMMENT.ID,
                    POST_COMMENT.REVIEW,
                    TAG.ID,
                    TAG.NAME,
                    USER_VOTE.ID,
                    USER_VOTE.VOTE_TYPE,
                    concat(
                        USER.FIRST_NAME,
                        inline(" "),
                        USER.LAST_NAME
                    )
                )
                .from(POST)
                .leftOuterJoin(POST_COMMENT).on(POST_COMMENT.POST_ID.eq(POST.ID))
                .leftOuterJoin(POST_TAG).on(POST_TAG.POST_ID.eq(POST.ID))
                .leftOuterJoin(TAG).on(TAG.ID.eq(POST_TAG.TAG_ID))
                .leftOuterJoin(USER_VOTE).on(USER_VOTE.COMMENT_ID.eq(POST_COMMENT.ID))
                .leftOuterJoin(USER).on(USER.ID.eq(USER_VOTE.USER_ID))
                .where(POST.ID.between(minPostId, maxPostId))
                .orderBy(POST.ID.asc())
                .fetchInto(FlatPostRecord.class);

            assertEquals(POST_COUNT * POST_COMMENT_COUNT * TAG_COUNT * VOTE_COUNT, posts.size());

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

        doInJOOQ(sql -> {
            List<PostRecord> posts = sql
                .select(
                    POST.ID,
                    POST.TITLE,
                    POST_COMMENT.ID,
                    POST_COMMENT.REVIEW,
                    TAG.ID,
                    TAG.NAME,
                    USER_VOTE.ID,
                    USER_VOTE.VOTE_TYPE,
                    concat(
                        USER.FIRST_NAME,
                        inline(" "),
                        USER.LAST_NAME
                    )
                )
                .from(POST)
                .leftOuterJoin(POST_COMMENT).on(POST_COMMENT.POST_ID.eq(POST.ID))
                .leftOuterJoin(POST_TAG).on(POST_TAG.POST_ID.eq(POST.ID))
                .leftOuterJoin(TAG).on(TAG.ID.eq(POST_TAG.TAG_ID))
                .leftOuterJoin(USER_VOTE).on(USER_VOTE.COMMENT_ID.eq(POST_COMMENT.ID))
                .leftOuterJoin(USER).on(USER.ID.eq(USER_VOTE.USER_ID))
                .where(POST.ID.between(minPostId, maxPostId))
                .orderBy(POST.ID.asc())
                .fetchInto(FlatPostRecord.class)
                .stream()
                .collect(
                    Collectors.collectingAndThen(
                        Collectors.toMap(
                            FlatPostRecord::postId,
                            record -> {
                                PostRecord post = new PostRecord(
                                    record.postId(),
                                    record.postTitle(),
                                    new ArrayList<>(),
                                    new ArrayList<>()
                                );

                                Long commentId = record.commentId();
                                if (commentId != null) {
                                    CommentRecord commentRecord = new CommentRecord(
                                        commentId,
                                        record.commentReview(),
                                        new ArrayList<>()
                                    );

                                    Long voteId = record.voteId();
                                    if (voteId != null) {
                                        commentRecord.votes().add(
                                            new UserVoteRecord(
                                                voteId,
                                                record.userName(),
                                                record.voteType()
                                            )
                                        );
                                    }
                                    post.comments().add(
                                        commentRecord
                                    );
                                }

                                Long tagId = record.tagId();
                                if (tagId != null) {
                                    post.tags().add(
                                        new TagRecord(
                                            tagId,
                                            record.tagName()
                                        )
                                    );
                                }

                                return post;
                            },
                            (PostRecord existing, PostRecord replacement) -> {
                                if(replacement.comments().size() == 1) {
                                    CommentRecord newCommentRecord = replacement.comments().get(0);
                                    CommentRecord existingCommentRecord = existing.comments().stream().filter(
                                        commentRecord -> commentRecord.id().equals(newCommentRecord.id())
                                    ).findAny().orElse(null);

                                    if(existingCommentRecord == null) {
                                        existing.comments().add(newCommentRecord);
                                    } else {
                                        if(newCommentRecord.votes().size() == 1) {
                                            UserVoteRecord newUserVoteRecord = newCommentRecord.votes().get(0);
                                            if(!existingCommentRecord.votes().contains(newUserVoteRecord)) {
                                                existingCommentRecord.votes().add(newUserVoteRecord);
                                            }
                                        }
                                    }
                                }
                                if(replacement.tags().size() == 1) {
                                    TagRecord newTagRecord = replacement.tags().get(0);
                                    if(!existing.tags().contains(newTagRecord)) {
                                        existing.tags().add(newTagRecord);
                                    }
                                }
                                return existing;
                            },
                            LinkedHashMap::new
                        ),
                        (Function<Map<Long, PostRecord>, List<PostRecord>>) map -> new ArrayList<>(map.values())
                    )
                );

            assertEquals(POST_COUNT, posts.size());
            PostRecord post = posts.get(0);
            assertEquals(POST_COMMENT_COUNT, post.comments().size());
            assertEquals(TAG_COUNT, post.tags().size());
            CommentRecord comment = post.comments().get(0);
            assertEquals(VOTE_COUNT, comment.votes().size());
        });
    }
}

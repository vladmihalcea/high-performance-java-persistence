package com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.record;

import com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.domain.VoteType;

/**
 * @author Vlad Mihalcea
 */
public record FlatPostRecord(
    Long postId,
    String postTitle,
    Long commentId,
    String commentReview,
    Long tagId,
    String tagName,
    Long voteId,
    VoteType voteType,
    String userName
) {
}

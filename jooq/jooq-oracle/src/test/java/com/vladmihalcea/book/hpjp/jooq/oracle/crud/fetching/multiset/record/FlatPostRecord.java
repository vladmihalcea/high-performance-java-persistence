package com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset.record;

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
    Integer voteScore,
    String userName
) {
}

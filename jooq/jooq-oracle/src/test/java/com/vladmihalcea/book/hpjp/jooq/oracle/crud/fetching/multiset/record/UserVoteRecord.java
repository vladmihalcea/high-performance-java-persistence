package com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset.record;

/**
 * @author Vlad Mihalcea
 */
public record UserVoteRecord(
    Long id,
    String userName,
    Integer userVote) {
}

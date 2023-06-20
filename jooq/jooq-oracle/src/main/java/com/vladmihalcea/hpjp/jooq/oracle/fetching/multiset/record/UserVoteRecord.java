package com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.record;

import com.vladmihalcea.hpjp.jooq.oracle.fetching.multiset.domain.VoteType;

/**
 * @author Vlad Mihalcea
 */
public record UserVoteRecord(
    Long id,
    String userName,
    VoteType userVote) {
}

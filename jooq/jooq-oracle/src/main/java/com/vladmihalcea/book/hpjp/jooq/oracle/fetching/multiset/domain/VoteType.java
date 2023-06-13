package com.vladmihalcea.book.hpjp.jooq.oracle.fetching.multiset.domain;

/**
 * @author Vlad Mihalcea
 */
public enum VoteType {
    UPVOTE(1),
    DOWNVOTE(-1),
    BOUNTY_50(50),
    BOUNTY_100(100),
    BOUNTY_500(500);

    final int score;

    VoteType(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}

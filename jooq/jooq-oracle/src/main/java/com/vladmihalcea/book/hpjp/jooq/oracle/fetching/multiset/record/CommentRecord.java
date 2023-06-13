package com.vladmihalcea.book.hpjp.jooq.oracle.fetching.multiset.record;

import java.util.List;
import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
public record CommentRecord(
    Long id,
    String review,
    List<UserVoteRecord> votes) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommentRecord)) return false;
        CommentRecord that = (CommentRecord) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

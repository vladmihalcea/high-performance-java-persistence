package com.vladmihalcea.book.hpjp.jooq.oracle.fetching.multiset.record;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public record PostRecord(
    Long id,
    String title,
    List<CommentRecord> comments,
    List<TagRecord> tags
) {
}

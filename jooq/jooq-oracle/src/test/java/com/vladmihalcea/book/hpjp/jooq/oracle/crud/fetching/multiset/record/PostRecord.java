package com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset.record;

import com.vladmihalcea.book.hpjp.jooq.oracle.crud.fetching.multiset.CommentRecord;

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

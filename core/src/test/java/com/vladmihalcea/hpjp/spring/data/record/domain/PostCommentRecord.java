package com.vladmihalcea.hpjp.spring.data.record.domain;

/**
 * @author Vlad Mihalcea
 */
public record PostCommentRecord(
    Long id,
    String review
) {
    public PostComment toPostComment() {
        return new PostComment()
            .setId(id)
            .setReview(review);
    }
}

package com.vladmihalcea.book.hpjp.hibernate.cache.query;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentSummary {

    private Long commentId;

    private String title;

    private String review;

    public PostCommentSummary(Long commentId, String title, String review) {
        this.commentId = commentId;
        this.title = title;
        this.review = review;
    }

    public Long getCommentId() {
        return commentId;
    }

    public String getTitle() {
        return title;
    }

    public String getReview() {
        return review;
    }
}

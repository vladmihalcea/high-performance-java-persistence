package com.vladmihalcea.book.hpjp.hibernate.fetching;

/**
 * <code>PostCommentSummary</code> - PostCommentSummary
 *
 * @author Vlad Mihalcea
 */
public class PostCommentSummary {

    private final Number id;
    private final String title;
    private final String review;

    public PostCommentSummary(Number id, String title, String review) {
        this.id = id;
        this.title = title;
        this.review = review;
    }

    public Number getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getReview() {
        return review;
    }
}

package com.vladmihalcea.book.hpjp.hibernate.fetching;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentSummary {

    private Number id;
    private String title;
    private String review;

    public PostCommentSummary(Number id, String title, String review) {
        this.id = id;
        this.title = title;
        this.review = review;
    }

    public PostCommentSummary() {}

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

package com.vladmihalcea.book.hpjp.hibernate.fetching;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentDTO {

    private final Long id;

    private final String review;

    private final String title;

    public PostCommentDTO(Long id, String review, String title) {
        this.id = id;
        this.review = review;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getReview() {
        return review;
    }

    public String getTitle() {
        return title;
    }
}

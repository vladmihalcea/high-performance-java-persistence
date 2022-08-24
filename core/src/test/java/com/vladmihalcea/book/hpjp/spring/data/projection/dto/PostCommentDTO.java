package com.vladmihalcea.book.hpjp.spring.data.projection.dto;

import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentDTO {

    private final Long id;

    private final String title;

    private final String review;

    public PostCommentDTO(Long id, String title, String review) {
        this.id = id;
        this.title = title;
        this.review = review;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getReview() {
        return review;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostCommentDTO)) return false;
        PostCommentDTO that = (PostCommentDTO) o;
        return Objects.equals(getId(), that.getId()) &&
               Objects.equals(getTitle(), that.getTitle()) &&
               Objects.equals(getReview(), that.getReview());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            getId(),
            getTitle(),
            getReview()
        );
    }
}

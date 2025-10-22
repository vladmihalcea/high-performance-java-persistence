package com.vladmihalcea.hpjp.spring.data.dto2entity.dto;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentDTO {
    private Long id;

    private String review;

    public Long getId() {
        return id;
    }

    public PostCommentDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getReview() {
        return review;
    }

    public PostCommentDTO setReview(String review) {
        this.review = review;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostCommentDTO)) return false;
        return id != null && id.equals(((PostCommentDTO) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

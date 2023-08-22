package com.vladmihalcea.hpjp.spring.data.masquerade.dto;

import com.vladmihalcea.hpjp.util.CryptoUtils;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentDTO {

    private final String id;

    private final String review;

    public PostCommentDTO(Long id, String review) {
        this.id = CryptoUtils.encrypt(id);
        this.review = review;
    }

    public String getId() {
        return id;
    }

    public String getReview() {
        return review;
    }
}

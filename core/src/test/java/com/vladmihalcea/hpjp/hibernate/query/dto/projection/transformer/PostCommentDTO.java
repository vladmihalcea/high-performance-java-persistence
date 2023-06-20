package com.vladmihalcea.hpjp.hibernate.query.dto.projection.transformer;

import com.vladmihalcea.hpjp.util.AbstractTest;

import java.util.Map;

/**
 * @author Vlad Mihalcea
 */
public class PostCommentDTO {
    public static final String ID_ALIAS = "pc_id";
    public static final String REVIEW_ALIAS = "pc_review";

    private Long id;

    private String review;

    public PostCommentDTO(Long id, String review) {
        this.id = id;
        this.review = review;
    }

    public PostCommentDTO(Object[] tuples, Map<String, Integer> aliasToIndexMap) {
        this.id = AbstractTest.longValue(tuples[aliasToIndexMap.get(ID_ALIAS)]);
        this.review = AbstractTest.stringValue(tuples[aliasToIndexMap.get(REVIEW_ALIAS)]);
    }

    public Long getId() {
        return id;
    }

    public void setId(Number id) {
        this.id = id.longValue();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }
}

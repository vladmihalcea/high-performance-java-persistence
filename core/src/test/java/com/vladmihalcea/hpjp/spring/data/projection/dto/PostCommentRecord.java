package com.vladmihalcea.hpjp.spring.data.projection.dto;

/**
 * @author Vlad Mihalcea
 */
public record PostCommentRecord(
    Long id,
    String title,
    String review
) {
}

package com.vladmihalcea.hpjp.spring.data.record.domain;

import java.util.List;

/**
 * @author Vlad Mihalcea
 */
public record PostRecord(
    Long id,
    String title,
    List<PostCommentRecord> comments
) {
    public Post toPost() {
        Post post = new Post()
            .setId(id)
            .setTitle(title);
        comments.forEach(comment -> post.addComment(comment.toPostComment()));
        return post;
    }
}

package com.vladmihalcea.book.hpjp.hibernate.criteria.blaze.bulk;

import jakarta.persistence.*;

/**
 * The {@link PostComment}
 *
 * @author Vlad Mihalcea
 * @since 1.x.y
 */
@Entity(name = "PostComment")
@Table(name = "post_comment")
public class PostComment extends PostModerate<PostComment> {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    private String message;

    public Long getId() {
        return id;
    }

    public PostComment setId(Long id) {
        this.id = id;
        return this;
    }

    public Post getPost() {
        return post;
    }

    public PostComment setPost(Post post) {
        this.post = post;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public PostComment setMessage(String message) {
        this.message = message;
        return this;
    }
}

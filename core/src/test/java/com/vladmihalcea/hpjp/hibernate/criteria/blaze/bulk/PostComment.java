package com.vladmihalcea.hpjp.hibernate.criteria.blaze.bulk;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
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

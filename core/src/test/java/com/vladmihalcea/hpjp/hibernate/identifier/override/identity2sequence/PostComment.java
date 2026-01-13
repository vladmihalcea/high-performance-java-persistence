package com.vladmihalcea.hpjp.hibernate.identifier.override.identity2sequence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "PostComment")
@Table(name = "post_comment")
public class PostComment extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    private String review;

    public String getReview() {
        return review;
    }

    public void setReview(String title) {
        this.review = title;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }
}

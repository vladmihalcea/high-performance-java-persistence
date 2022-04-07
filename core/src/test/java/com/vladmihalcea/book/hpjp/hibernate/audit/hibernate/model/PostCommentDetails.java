package com.vladmihalcea.book.hpjp.hibernate.audit.hibernate.model;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "PostCommentDetails")
@Table(name = "post_comment_details")
public class PostCommentDetails implements Auditable<Long> {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private PostComment comment;

    private int votes;

    public Long getId() {
        return id;
    }

    public PostCommentDetails setId(Long id) {
        this.id = id;
        return this;
    }

    public PostComment getComment() {
        return comment;
    }

    public PostCommentDetails setComment(PostComment comment) {
        this.comment = comment;
        return this;
    }

    public int getVotes() {
        return votes;
    }

    public PostCommentDetails setVotes(int votes) {
        this.votes = votes;
        return this;
    }
}

package com.vladmihalcea.hpjp.spring.data.query.window.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post_comment")
public class PostComment {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    private PostComment parent;

    private String review;

    @Enumerated(EnumType.ORDINAL)
    private Status status;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    private int votes;

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

    public PostComment getParent() {
        return parent;
    }

    public PostComment setParent(PostComment parent) {
        this.parent = parent;
        return this;
    }

    public String getReview() {
        return review;
    }

    public PostComment setReview(String review) {
        this.review = review;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public PostComment setStatus(Status status) {
        this.status = status;
        return this;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public PostComment setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public int getVotes() {
        return votes;
    }

    public PostComment setVotes(int votes) {
        this.votes = votes;
        return this;
    }

    public enum Status {
        PENDING,
        APPROVED,
        SPAM;
    }
}

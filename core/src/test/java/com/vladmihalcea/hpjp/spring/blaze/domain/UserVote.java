package com.vladmihalcea.hpjp.spring.blaze.domain;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "UserVote")
@Table(name = "user_vote")
public class UserVote {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private PostComment comment;

    private int score;

    public Long getId() {
        return id;
    }

    public UserVote setId(Long id) {
        this.id = id;
        return this;
    }

    public User getUser() {
        return user;
    }

    public UserVote setUser(User user) {
        this.user = user;
        return this;
    }

    public PostComment getComment() {
        return comment;
    }

    public UserVote setComment(PostComment comment) {
        this.comment = comment;
        return this;
    }

    public int getScore() {
        return score;
    }

    public UserVote setScore(int score) {
        this.score = score;
        return this;
    }
}

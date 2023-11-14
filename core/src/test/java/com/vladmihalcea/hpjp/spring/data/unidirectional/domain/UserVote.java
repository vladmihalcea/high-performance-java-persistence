package com.vladmihalcea.hpjp.spring.data.unidirectional.domain;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "user_votes")
public class UserVote extends VersionedEntity {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        foreignKey = @ForeignKey(
            name = "FK_user_vote_user_id"
        )
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        foreignKey = @ForeignKey(
            name = "FK_user_vote_comment_id"
        )
    )
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

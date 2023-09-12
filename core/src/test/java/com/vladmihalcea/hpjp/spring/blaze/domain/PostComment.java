package com.vladmihalcea.hpjp.spring.blaze.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "PostComment")
@Table(name = "post_comment")
public class PostComment {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post post;

    private String review;

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserVote> votes = new ArrayList<>();

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

    public String getReview() {
        return review;
    }

    public PostComment setReview(String review) {
        this.review = review;
        return this;
    }

    public List<UserVote> getVotes() {
        return votes;
    }

    public PostComment addVote(UserVote vote) {
        votes.add(vote);
        vote.setComment(this);
        return this;
    }
}

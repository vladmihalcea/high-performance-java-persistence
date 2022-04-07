package com.vladmihalcea.book.hpjp.hibernate.query.subquery;

import jakarta.persistence.*;
import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "PostComment")
@Table(name = "post_comment")
public class PostComment {

    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_on")
    private Date createdOn = new Date();

    private String review;

    private int score;

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

    public Date getCreatedOn() {
        return createdOn;
    }

    public PostComment setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public int getScore() {
        return score;
    }

    public PostComment setScore(int score) {
        this.score = score;
        return this;
    }
}

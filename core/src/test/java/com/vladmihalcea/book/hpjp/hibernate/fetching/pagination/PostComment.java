package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "PostComment")
@Table(name = "post_comment")
public class PostComment implements Identifiable<Long> {

    @Id
    private Long id;

    @ManyToOne
    private Post post;

    private String review;

    @Column(name = "created_on")
    private Timestamp createdOn;

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

    public PostComment setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
        return this;
    }
}

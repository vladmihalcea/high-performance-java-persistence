package com.vladmihalcea.book.hpjp.hibernate.fetching.pagination;

import com.vladmihalcea.book.hpjp.hibernate.identifier.Identifiable;

import javax.persistence.*;
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

    public void setId(Long id) {
        this.id = id;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Timestamp createdOn) {
        this.createdOn = createdOn;
    }
}

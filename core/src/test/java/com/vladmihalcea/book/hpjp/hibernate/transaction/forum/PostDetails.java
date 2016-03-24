package com.vladmihalcea.book.hpjp.hibernate.transaction.forum;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post_details")
public class PostDetails {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "created_on")
    private Date createdOn;

    @Column(name = "created_by")
    private String createdBy;

    public PostDetails() {
        createdOn = new Date();
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id")
    @MapsId
    private Post post;

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

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}

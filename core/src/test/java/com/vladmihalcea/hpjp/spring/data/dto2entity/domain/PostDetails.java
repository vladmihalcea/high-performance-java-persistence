package com.vladmihalcea.hpjp.spring.data.dto2entity.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "PostDetails")
@Table(name = "post_details")
@DynamicUpdate
public class PostDetails {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private Post post;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;

    @Column(name = "updated_by")
    private String updatedBy;

    public Long getId() {
        return id;
    }

    public PostDetails setId(Long id) {
        this.id = id;
        return this;
    }

    public Post getPost() {
        return post;
    }

    public PostDetails setPost(Post post) {
        this.post = post;
        return this;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public PostDetails setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public PostDetails setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public PostDetails setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
        return this;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public PostDetails setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostDetails)) return false;
        return id != null && id.equals(((PostDetails) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

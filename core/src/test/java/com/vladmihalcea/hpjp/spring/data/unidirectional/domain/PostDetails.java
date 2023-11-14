package com.vladmihalcea.hpjp.spring.data.unidirectional.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post_details")
public class PostDetails extends VersionedEntity {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(
        name = "id",
        foreignKey = @ForeignKey(
            name = "FK_post_details_id"
        )
    )
    private Post post;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    private String createdBy;

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
        this.id = post.getId();
        if (getVersion() == null) {
            setVersion(post.getVersion());
        }
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
}

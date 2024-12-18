package com.vladmihalcea.hpjp.hibernate.mapping.types;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post")
public class Post {

    @Id
    private Integer id;

    private String title;

    @Enumerated(EnumType.ORDINAL)
    private PostStatus status;

    @Embedded
    private CreationDetails creationDetails;

    public Integer getId() {
        return id;
    }

    public Post setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Post setTitle(String title) {
        this.title = title;
        return this;
    }

    public PostStatus getStatus() {
        return status;
    }

    public Post setStatus(PostStatus status) {
        this.status = status;
        return this;
    }

    public CreationDetails getCreationDetails() {
        return creationDetails;
    }

    public Post setCreationDetails(CreationDetails creation) {
        this.creationDetails = creation;
        return this;
    }
}

package com.vladmihalcea.hpjp.spring.batch.domain;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post")
public class Post {

    @Id
    private Long id;

    private String title;

    @Enumerated(EnumType.ORDINAL)
    private PostStatus status;

    public Long getId() {
        return id;
    }

    public Post setId(Long id) {
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
}

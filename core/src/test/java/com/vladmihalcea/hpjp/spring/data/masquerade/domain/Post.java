package com.vladmihalcea.hpjp.spring.data.masquerade.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
public class Post {

    @Id
    private Long id;

    private String title;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

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

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public Post setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
        return this;
    }
}

package com.vladmihalcea.hpjp.spring.data.unidirectional.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "posts")
public class Post extends VersionedEntity {

    @Id
    private Long id;

    @Column(length = 250)
    private String title;

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
}

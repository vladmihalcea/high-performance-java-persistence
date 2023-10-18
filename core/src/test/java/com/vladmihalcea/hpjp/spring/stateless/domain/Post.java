package com.vladmihalcea.hpjp.spring.stateless.domain;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "post")
public class Post extends AbstractPost<Post> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public Post setId(Long id) {
        this.id = id;
        return this;
    }
}

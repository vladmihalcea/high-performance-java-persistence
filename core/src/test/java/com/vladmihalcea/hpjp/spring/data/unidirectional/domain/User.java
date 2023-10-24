package com.vladmihalcea.hpjp.spring.data.unidirectional.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "blog_user")
public class User {

    @Id
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public User setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public User setName(String name) {
        this.name = name;
        return this;
    }
}

package com.vladmihalcea.book.hpjp.spring.data.query.multibag.domain;

import com.vladmihalcea.book.hpjp.hibernate.fetching.multiple.EagerFetchingMultipleBagTest;

import javax.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Tag")
@Table(name = "tag")
public class Tag {

    @Id
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public Tag setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Tag setName(String name) {
        this.name = name;
        return this;
    }
}

package com.vladmihalcea.book.hpjp.hibernate.forum;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity
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

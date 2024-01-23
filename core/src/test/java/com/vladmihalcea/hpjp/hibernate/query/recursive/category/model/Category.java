package com.vladmihalcea.hpjp.hibernate.query.recursive.category.model;

import jakarta.persistence.*;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue
    private Short id;

    @Column(length = 25)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category parent;

    public Short getId() {
        return id;
    }

    public Category setId(Short id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Category setName(String name) {
        this.name = name;
        return this;
    }

    public Category getParent() {
        return parent;
    }

    public Category setParent(Category parent) {
        this.parent = parent;
        return this;
    }
}

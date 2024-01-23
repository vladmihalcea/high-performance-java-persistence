package com.vladmihalcea.hpjp.hibernate.query.recursive.category.model;

import com.blazebit.persistence.CTE;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author Vlad Mihalcea
 */
@CTE
@Entity
public class CategoryView {

    @Id
    private Short id;

    private String name;

    @ManyToOne
    private Category parent;

    public Short getId() {
        return id;
    }

    public CategoryView setId(Short id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public CategoryView setName(String name) {
        this.name = name;
        return this;
    }

    public Category getParent() {
        return parent;
    }

    public CategoryView setParent(Category parent) {
        this.parent = parent;
        return this;
    }
}

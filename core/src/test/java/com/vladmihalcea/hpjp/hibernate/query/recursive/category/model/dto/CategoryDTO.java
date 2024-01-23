package com.vladmihalcea.hpjp.hibernate.query.recursive.category.model.dto;

/**
 * @author Vlad Mihalcea
 */
public class CategoryDTO {

    private Short id;

    private String name;

    private CategoryDTO parent;

    public Short getId() {
        return id;
    }

    public CategoryDTO(Short id) {
        this.id = id;
    }

    public CategoryDTO(Short id, String name, Short parentId) {
        this.id = id;
        this.name = name;
        this.parent = parentId != null ? new CategoryDTO(parentId) : null;
    }

    public CategoryDTO setId(Short id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public CategoryDTO setName(String name) {
        this.name = name;
        return this;
    }

    public CategoryDTO getParent() {
        return parent;
    }

    public CategoryDTO setParent(CategoryDTO parent) {
        this.parent = parent;
        return this;
    }

    public CategoryDTO findByParentId(Short id) {
        if (parent != null) {
            if (parent.id.equals(id)) {
                return this;
            } else {
                return parent.findByParentId(id);
            }
        }
        return null;
    }
}

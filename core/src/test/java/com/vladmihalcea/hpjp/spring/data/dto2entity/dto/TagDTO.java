package com.vladmihalcea.hpjp.spring.data.dto2entity.dto;

import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
public class TagDTO {

    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public TagDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public TagDTO setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagDTO tagDTO)) return false;
        return Objects.equals(getName(), tagDTO.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}

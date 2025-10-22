package com.vladmihalcea.hpjp.spring.data.dto2entity.dto;

import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
public class PostDetailsDTO {
    private Long id;

    private LocalDateTime createdOn;

    private String createdBy;

    public Long getId() {
        return id;
    }

    public PostDetailsDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public PostDetailsDTO setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public PostDetailsDTO setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostDetailsDTO)) return false;
        return id != null && id.equals(((PostDetailsDTO) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

package com.vladmihalcea.hpjp.spring.stateless.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
@MappedSuperclass
public abstract class AbstractPost<T extends AbstractPost> {

    private String title;

    @Column(name = "created_on")
    private LocalDateTime createdOn = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn = LocalDateTime.now();

    @Column(name = "updated_by")
    private String updatedBy;

    @Version
    private Short version;

    public String getTitle() {
        return title;
    }

    public T setTitle(String title) {
        this.title = title;
        return (T) this;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public T setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
        return (T) this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public T setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return (T) this;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public T setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
        return (T) this;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public T setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
        return (T) this;
    }

    public Short getVersion() {
        return version;
    }

    public T setVersion(Short version) {
        this.version = version;
        return (T) this;
    }
}

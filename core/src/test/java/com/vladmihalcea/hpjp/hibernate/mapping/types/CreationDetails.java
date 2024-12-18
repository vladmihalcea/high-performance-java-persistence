package com.vladmihalcea.hpjp.hibernate.mapping.types;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
@Embeddable
public class CreationDetails {

    @Column(name = "created_on")
    private LocalDateTime createdOn = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public CreationDetails setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public CreationDetails setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }
}

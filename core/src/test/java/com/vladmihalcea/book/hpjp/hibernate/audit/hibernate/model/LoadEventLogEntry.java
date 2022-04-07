package com.vladmihalcea.book.hpjp.hibernate.audit.hibernate.model;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "LoadEventLogEntry")
@Table(name = "load_event_log")
public class LoadEventLogEntry {

    @Id
    @GeneratedValue
    private Long id;

    @CreationTimestamp
    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    private String createdBy;

    private String entityName;

    private String entityId;

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LoadEventLogEntry setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    public LoadEventLogEntry setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public String getEntityId() {
        return entityId;
    }

    public LoadEventLogEntry setEntityId(String entityId) {
        this.entityId = entityId;
        return this;
    }
}

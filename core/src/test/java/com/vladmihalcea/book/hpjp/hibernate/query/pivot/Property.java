package com.vladmihalcea.book.hpjp.hibernate.query.pivot;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 *
 * @author Vlad Mihalcea
 */
@Entity
public class Property {

    @EmbeddedId
    private PropertyId id;

    @Column(name = "property_value")
    private String value;

    public PropertyId getId() {
        return id;
    }

    public void setId(PropertyId id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

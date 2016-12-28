package com.vladmihalcea.book.hpjp.hibernate.query.pivot;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

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

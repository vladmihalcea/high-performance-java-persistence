package com.vladmihalcea.book.hpjp.hibernate.query.pivot;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author Vlad Mihalcea
 */
@Embeddable
public class PropertyId implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    private Component component;

    @Column(name = "property_name")
    private String name;

    public PropertyId() {
    }

    public PropertyId(Service service, Component component, String name) {
        this.service = service;
        this.component = component;
        this.name = name;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyId)) return false;
        PropertyId that = (PropertyId) o;
        return Objects.equals(service, that.service) &&
                Objects.equals(component, that.component) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, component, name);
    }
}

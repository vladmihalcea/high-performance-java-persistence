package com.vladmihalcea.book.hpjp.hibernate.query.pivot;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity
public class Service {

    @Id
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

package com.vladmihalcea.hpjp.hibernate.identifier.override.identity2sequence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Vlad Mihalcea
 */
@Entity(name = "Post")
@Table(name = "post")
public class Post extends AbstractEntity {

    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}

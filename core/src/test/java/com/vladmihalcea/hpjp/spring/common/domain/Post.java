package com.vladmihalcea.hpjp.spring.common.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(
    name = "post",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_POST_SLUG",
        columnNames = "slug"
    )
)
public class Post {

    @Id
    @GeneratedValue
    private Long id;

    private String title;

    @NaturalId
    private String slug;

    @Version
    private short version;

    public Long getId() {
        return id;
    }

    public Post setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Post setTitle(String title) {
        this.title = title;
        return this;
    }

    public Post setSlug(String slug) {
        this.slug = slug;
        return this;
    }
}

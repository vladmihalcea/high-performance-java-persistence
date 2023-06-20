package com.vladmihalcea.hpjp.spring.data.crud.domain;

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
    private Long id;

    private String title;

    @NaturalId
    private String slug;

    @Enumerated(EnumType.ORDINAL)
    @Column(columnDefinition = "NUMERIC(2)")
    private PostStatus status;

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

    public String getSlug() {
        return slug;
    }

    public Post setSlug(String slug) {
        this.slug = slug;
        return this;
    }

    public PostStatus getStatus() {
        return status;
    }

    public Post setStatus(PostStatus status) {
        this.status = status;
        return this;
    }
}

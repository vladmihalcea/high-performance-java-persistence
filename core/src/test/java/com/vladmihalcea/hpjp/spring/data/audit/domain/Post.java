package com.vladmihalcea.hpjp.spring.data.audit.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import org.hibernate.envers.Audited;

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
@Audited
public class Post {

    @Id
    @GeneratedValue
    private Long id;

    @Column(length = 100)
    private String title;

    @NaturalId
    @Column(length = 75)
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

    @Override
    public String toString() {
        return "Post{" +
               "id=" + id +
               ", title='" + title + '\'' +
               ", slug='" + slug + '\'' +
               ", status=" + status +
               '}';
    }
}

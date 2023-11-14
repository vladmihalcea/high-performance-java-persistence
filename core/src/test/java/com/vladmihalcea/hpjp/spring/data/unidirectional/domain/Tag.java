package com.vladmihalcea.hpjp.spring.data.unidirectional.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "tags")
public class Tag extends VersionedEntity {

    @Id
    @GeneratedValue
    private Long id;

    @NaturalId
    @Column(length = 40)
    private String name;

    public Long getId() {
        return id;
    }

    public Tag setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Tag setName(String name) {
        this.name = name;
        return this;
    }
}

package com.vladmihalcea.hpjp.spring.transaction.contract.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * @author Vlad Mihalcea
 */
@Entity
@Table(name = "contract")
public class Contract {

    @Id
    private Long id;

    private String title;

    @Version
    private Short version;

    public Long getId() {
        return id;
    }

    public Contract setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Contract setTitle(String title) {
        this.title = title;
        return this;
    }
}
